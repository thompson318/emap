from datetime import timedelta

import numpy as np
import pandas as pd
import streamlit as st
import altair as alt
import database_utils
import waveform_utils

# for par in all_params.itertuples():
#     data = get_data_single_stream(par.visit_observation_type_id, par.source_location)
#     st.multiselect("Flerp", list(data['source_location']))

all_params = database_utils.get_all_params()

def reset_times(gr):
    return gr.assign(observation_datetime=(
            gr.observation_datetime + gr.idx_within_group.apply(
        lambda idx: pd.Timedelta(seconds=idx/gr.iloc[0].sampling_rate)
    )))


# locations = st.multiselect("Flerp2", list(range(5)))
location = st.selectbox("Choose location", list(all_params['source_location']))
stream = st.selectbox("Choose stream", set(all_params['visit_observation_type_id']))

# This is currently mixing up all the streams!!
print("location = " + str(location))
print("stream = " + str(stream))
if not location:
    st.error("Please select a location")
elif not stream:
    st.error("Please select a stream")
else:
    blerp = (all_params['source_location'] == location) & (all_params['visit_observation_type_id'] == stream)
    par = all_params[blerp]
    print("par" + str(par))
    print("par.iloc[0]" + str(par.iloc[0]))
    # print("par[0]" + str(par[0]))
    min_time, max_time = database_utils.get_min_max_time_for_single_stream(int(par.iloc[0].visit_observation_type_id), par.iloc[0].source_location)
    min_time = min_time.to_pydatetime()
    max_time = max_time.to_pydatetime()
    print(f"min={min_time}, max={max_time}")
    st.write(f"{min_time}, {max_time}")
    if min_time is None:
        st.error("No data for location found")

    # graph_one_time = st.slider("time?", min_value=min_time, max_value=max_time)
    select_options = []
    t = min_time
    while t <= max_time:
        select_options.append(t)
        t += timedelta(seconds=1)

    graph_min_time, graph_max_time = st.select_slider("time interval?", options=select_options, value=[min_time, max_time])
    data = database_utils.get_data_single_stream(int(par.iloc[0].visit_observation_type_id), par.iloc[0].source_location,
                                                 min_time=graph_min_time, max_time=graph_max_time)
    print(data.columns)
    # data.explode('values_array')
    # # explode into one row per data point, and adjust the observation datetimes
    # one_per_row = data.explode('values_array')
    # one_per_row['idx_within_group'] = one_per_row.groupby(level=0).cumcount()
    one_per_row_reset_times = waveform_utils.explode_values_array(data)

    # data['values_array'] = data['values_array'].apply(lambda l: np.array(l))
    chart = (
        alt.Chart(one_per_row_reset_times)
        .mark_area(opacity=0.9)
        .encode(
            x="observation_datetime",
            y=alt.Y("value", stack=None),
            # color="Region:N",
        ).interactive()
        # .add_params(
        #     alt.selection_interval(bind='scales')
        # )
    )
    # st.scatter_chart(one_per_row_reset_times, y='value', size=1, on_select='rerun')
    st.altair_chart(chart, use_container_width=True) # , on_select='rerun')

