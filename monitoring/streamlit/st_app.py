from datetime import timedelta, datetime

import pandas as pd
import streamlit as st
import altair as alt
import database_utils

st.set_page_config(layout="wide")

st_top_controls = st.container()
st_bottom_controls = st.container()
st_graph_area = st.container()
st_info_box = st.container()
with st_info_box:
    st.write(f"Schema: {database_utils.database_schema}")
with st_top_controls:
    top_cols = st.columns(2)
with st_bottom_controls:
    bottom_cols = st.columns(2, gap='medium')

all_params = database_utils.get_all_params()

unique_streams_list = all_params.apply(lambda r: (r['visit_observation_type_id'], r['name']), axis=1).drop_duplicates().tolist()
unique_streams = dict(unique_streams_list)
if len(unique_streams_list) != len(unique_streams):
    # the DB schema should ensure this doesn't happen, but check
    with st_graph_area:
        st.error(f"WARNING: apparent ambiguous mapping in {unique_streams_list}")

with top_cols[0]:
    location = st.selectbox("Choose location", sorted(set(all_params['source_location'])))
with top_cols[1]:
    stream_id = st.selectbox("Choose stream", unique_streams.keys(), format_func=lambda i: unique_streams[i])


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
    stream_label = unique_streams[stream_id]
    stream_filter = (all_params['source_location'] == location) & (all_params['visit_observation_type_id'] == stream_id)
    par = all_params[stream_filter]
    min_time, max_time = database_utils.get_min_max_time_for_single_stream(int(par.iloc[0].visit_observation_type_id), par.iloc[0].source_location)
    min_time = min_time.to_pydatetime()
    max_time = max_time.to_pydatetime()
    print(f"min={min_time}, max={max_time}")
    if min_time is None:
        st.error("No data for location found")

    with bottom_cols[0]:
        graph_start_time = st.slider("Start time", min_value=min_time, max_value=max_time, step=timedelta(seconds=1),
                                     format="")
    with bottom_cols[1]:
        graph_width_seconds = st.slider("Chart duration (seconds)", min_value=1, max_value=30, value=30)

    graph_end_time = graph_start_time + timedelta(seconds=graph_width_seconds)
    data = database_utils.get_data_single_stream_rounded(int(par.iloc[0].visit_observation_type_id), par.iloc[0].source_location,
                                                 min_time=graph_start_time, max_time=graph_end_time)
    trimmed = data[data['observation_datetime'].between(graph_start_time, graph_end_time)]
    waveform_units = trimmed['unit'].drop_duplicates().tolist()
    if len(waveform_units) > 1:
        with st_graph_area:
            st.error(f"duplicate units: {waveform_units}")
        waveform_unit = "n/a"
    elif len(waveform_units) == 0:
        with st_graph_area:
            st.error(f"no data over the given time period, try selecting another time")
        waveform_unit = "n/a"
    else:
        waveform_unit = waveform_units[0]


    chart = (
        alt.Chart(trimmed, width=1100, height=600)
        .mark_point(opacity=0.9)
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
    with st_graph_area:
        st.altair_chart(chart, use_container_width=True)

