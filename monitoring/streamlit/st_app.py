from datetime import timedelta, datetime
import time

import pandas as pd
import streamlit as st
import altair as alt
import database_utils

st.set_page_config(layout="wide")

st_top_controls = st.container()
st_bottom_controls = st.container()
st_graph_area = st.container()
st_info_box = st.container()
st_info_box.write(f"Schema: {database_utils.database_schema}")
top_cols = st_top_controls.columns(4)
bottom_cols = st_bottom_controls.columns(1, gap='medium')

all_params = database_utils.get_all_params()

unique_streams_list = all_params.apply(lambda r: (r['visit_observation_type_id'], r['name']), axis=1).drop_duplicates().tolist()
unique_streams = dict(unique_streams_list)
if len(unique_streams_list) != len(unique_streams):
    # the DB schema should ensure this doesn't happen, but check
    st_graph_area.error(f"WARNING: apparent ambiguous mapping in {unique_streams_list}")

location = top_cols[0].selectbox("Choose location", sorted(set(all_params['source_location'])))
stream_id = top_cols[1].selectbox("Choose stream", unique_streams.keys(), format_func=lambda i: unique_streams[i])


def reset_times(gr):
    return gr.assign(observation_datetime=(
            gr.observation_datetime + gr.idx_within_group.apply(
        lambda idx: pd.Timedelta(seconds=idx/gr.iloc[0].sampling_rate)
    )))


print(f"location = {location}, stream_id = {stream_id}")


if not location:
    st.error("Please select a location")
elif not stream_id:
    st.error("Please select a stream")
else:
    if top_cols[2].button("Re-check DB"):
        st.cache_data.clear()

    min_time, max_time = database_utils.get_min_max_time_for_single_stream(int(stream_id), location)
    min_time = min_time.to_pydatetime()
    max_time = max_time.to_pydatetime()

    # (re-)initialise slider value if not known or if the bounds have changed so that it is now outside them
    if 'slider_value' not in st.session_state or not min_time <= st.session_state.slider_value <= max_time:
        st.session_state.slider_value = max(min_time, max_time - timedelta(seconds=15))
    print(f"New bounds for stream {stream_id}, location {location}: min={min_time}, max={max_time}, value={st.session_state.slider_value}")
    # BUG: error is given if there is exactly one point so min_time == max_time
    graph_start_time = bottom_cols[0].slider("Start time",
                                             min_value=min_time, max_value=max_time,
                                             value=st.session_state.slider_value,
                                             step=timedelta(seconds=10), format="")
    st.session_state.slider_value = graph_start_time
    if min_time is None:
        st.error("No data for location+stream found")

    graph_width_seconds = top_cols[3].slider("Chart width (seconds)", min_value=1, max_value=30, value=30)

    graph_end_time = graph_start_time + timedelta(seconds=graph_width_seconds)
    data = database_utils.get_data_single_stream_rounded(int(stream_id), location,
                                                 min_time=graph_start_time, max_time=graph_end_time)
    trimmed = data[data['observation_datetime'].between(graph_start_time, graph_end_time)]
    waveform_units = trimmed['unit'].drop_duplicates().tolist()
    if len(waveform_units) > 1:
        st_graph_area.error(f"duplicate units: {waveform_units}")
        waveform_unit = "n/a"
    elif len(waveform_units) == 0:
        st_graph_area.error(f"no data over the given time period, try selecting another time")
        waveform_unit = "n/a"
    else:
        waveform_unit = waveform_units[0]

    stream_label = unique_streams[stream_id]
    chart = (
        alt.Chart(trimmed, width=1100, height=600)
        # unfortunately the line continues over gaps in the data, but points are too ugly so stick with this for now
        .mark_line(opacity=0.9)
        .encode(
            x=alt.X("observation_datetime",
                    title="Observation datetime",
                    # timeUnit="hoursminutesseconds", # using this causes a weird data corruption problem
                    scale=alt.Scale(type="utc"),
                    axis=alt.Axis(tickCount="millisecond",
                                  tickColor="red",
                                  tickBand="center",
                                  titleFontSize=24,
                                  ticks=True),
                    ),
            y=alt.Y("waveform_value",
                    title=f"{stream_label} ({waveform_unit})",
                    stack=None,
                    axis=alt.Axis(
                        titleFontSize=24,
                    )),
            # color="Region:N",
        )
        #.interactive()
        # .add_params(
        #     alt.selection_interval(bind='scales')
        # )
    )
    st_graph_area.altair_chart(chart, use_container_width=True)

