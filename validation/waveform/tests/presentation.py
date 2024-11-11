# -*- coding: utf-8 -*-
# ---
# jupyter:
#   jupytext:
#     notebook_metadata_filter: -jupytext.text_representation.jupytext_version,-kernelspec
#     text_representation:
#       extension: .py
#       format_name: percent
#       format_version: '1.3'
# ---

# %%

# %% [markdown]
# yadda yadda

# %%
from datetime import datetime
from functools import lru_cache

import pandas as pd
import sqlalchemy
import psycopg2
import pandas as pd
import soundfile
import matplotlib.pyplot as plt
import numpy as np
from scipy.fft import fft
import database_utils

# %%
all_params = database_utils.get_all_params()

# %%
@lru_cache
def get_data_single_stream(visit_observation_type_id, source_location):
    params = (visit_observation_type_id, source_location)
    con = database_utils.engine.connect()
    data = pd.read_sql_query(database_utils.SET_SEARCH_PATH +
                             """
                             SELECT *
                             FROM WAVEFORM
                             WHERE visit_observation_type_id = %s AND source_location = %s
                             ORDER BY observation_datetime
                             """, con, params=params)
    return data

# %%
# For keeping output files from different runs separate
date_str = datetime.now().strftime('%Y%m%dT%H%M%S')
print(date_str)



# %%
def to_ogg():
    date_str = datetime.now().strftime('%Y%m%dT%H%M%S')
    for par in all_params.itertuples():
        data = get_data_single_stream(par.visit_observation_type_id, par.source_location)
        all_points = []
        data['values_array'].apply(lambda va: all_points.extend(va))
        print(f"PRE  max={max(all_points)}, min={min(all_points)}")
        print(data.shape[0])
        print(len(all_points))
        all_points = [a/1000 for a in all_points]
        print(f"POST max={max(all_points)}, min={min(all_points)}")
        for sampling_rate in [88200]:
            outfile = f"output/output_{date_str}_{par.visit_observation_type_id}_{par.source_location}_{sampling_rate}.ogg"
            soundfile.write(outfile, all_points, sampling_rate, format='OGG')


# %%
def get_distinct_sampling_rate(data):
    unique_sampling_rate = data['sampling_rate'].unique()
    assert len(unique_sampling_rate) == 1
    return unique_sampling_rate[0]

# %%
def do_fft(all_points, sampling_rate):
    sample_spacing = 1 / sampling_rate
    # fft
    all_points_centered = all_points - np.mean(all_points)
    fft_values = fft(all_points_centered)
    frequencies = np.fft.fftfreq(len(fft_values), sample_spacing)
    # use magnitude of complex fft values
    return all_points_centered, np.abs(fft_values), frequencies


# %%
def plot_waveform(par, max_seconds=30):
    # global plot_df, data, all_points_centered, abs_fft_values, frequencies
    data = get_data_single_stream(par.visit_observation_type_id, par.source_location)
    sampling_rate = get_distinct_sampling_rate(data)
    all_points = []
    data['values_array'].apply(lambda va: all_points.extend(va))
    # use only first 30 seconds
    all_points_trimmed = all_points[:sampling_rate * max_seconds]
    print(f"sampling rate {sampling_rate}, data {len(all_points)} -> {len(all_points_trimmed)}")
    all_points_centered, abs_fft_values, frequencies = do_fft(all_points_trimmed, sampling_rate)
    fig, ax = plt.subplots(1, 2, figsize=(10, 5))
    print(f"|points| = {len(all_points_centered)}, |fft_vals| = {len(abs_fft_values)}, |frequencies|/2 = {len(frequencies)/2}")
    # sampling rate / 2 is the absolute upper limit, but
    # it's unlikely the real frequencies are anywhere near that
    n = len(frequencies) // 8
    plot_df = pd.DataFrame(dict(freq=frequencies[:n], vals=abs_fft_values[:n]))
    ax[0].set_xlabel('freq')
    ax[0].set_ylabel('mag')
    ax[0].plot(plot_df['freq'], plot_df['vals'])
    idx_max = plot_df['vals'].idxmax()
    max_row = plot_df.loc[idx_max]
    print(max_row)
    # make sure it's more than the message length *and* sql array cardinality
    max_points_to_plot = 12000
    points_to_plot = min(max_points_to_plot, len(all_points_centered))
    ax[1].set_xlabel('sample num')
    ax[1].set_ylabel('waveform value')
    ax[1].plot(range(points_to_plot), all_points_centered[:points_to_plot])
    plt.show()
    outfile = f"validation_output/graph_{date_str}_{par.visit_observation_type_id}_{par.source_location}.png"
    plt.savefig(outfile)


# %%
# %matplotlib inline
for par in all_params.itertuples():
    if plot_waveform(par):
        break


# %%
