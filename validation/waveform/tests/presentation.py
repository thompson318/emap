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

database_url = 'postgresql+psycopg2://inform_user:inform@localhost:5433/fakeuds'
schema = "uds_schema"
search_path_preamble = f"set search_path to {schema};"
engine = sqlalchemy.create_engine(database_url)

# put in fixture
con = engine.connect()

all_params = pd.read_sql_query(search_path_preamble +
                               """
                               SELECT DISTINCT visit_observation_type_id, source_location
                               FROM WAVEFORM
                               """, con)
print(all_params)
print("!!!")


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

def to_png():
    date_str = datetime.now().strftime('%Y%m%dT%H%M%S')
    for par in all_params.itertuples():
        data = get_data_single_stream(par.visit_observation_type_id, par.source_location)
        all_points = []
        data['values_array'].apply(lambda va: all_points.extend(va))
        # fft
        fft_values = fft(all_points)
        app_points_centered = all_points - np.mean(all_points)
        unique_sampling_rate = data['sampling_rate'].unique()
        assert len(unique_sampling_rate) == 1
        sample_spacing = 1 / unique_sampling_rate[0]
        frequencies = np.fft.fftfreq(len(fft_values), sample_spacing)
        # use magnitude of complex fft values
        plot_df = pd.DataFrame(dict(freq=frequencies, vals=np.abs(fft_values)))
        plt.cla()
        plt.plot(plot_df)
        idx_max = plot_df['vals'].idxmax()
        max_row = plot_df.loc[idx_max]
        print(max_row)
        # plt.plot(frequencies, fft_values)
        outfile = f"output/fft_{date_str}_{par.visit_observation_type_id}_{par.source_location}.png"
        plt.savefig(outfile)
        # make sure it's more than the message length *and* sql array cardinality
        points_to_plot = 6000
        plt.cla()
        plt.plot(range(points_to_plot), all_points[:points_to_plot])
        outfile = f"output/graph_{date_str}_{par.visit_observation_type_id}_{par.source_location}.png"
        plt.savefig(outfile)
        break


@lru_cache
def get_data_single_stream(visit_observation_type_id, source_location):
    params = (visit_observation_type_id, source_location)
    data = pd.read_sql_query(search_path_preamble +
                             """
                             SELECT *
                             FROM WAVEFORM
                             WHERE visit_observation_type_id = %s AND source_location = %s
                             ORDER BY observation_datetime
                             """, con, params=params)
    return data


if __name__ == "__main__":
    # to_ogg()
    to_png()
