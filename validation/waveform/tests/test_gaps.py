import os

import pytest
from pytest import approx
import pandas as pd
import sqlalchemy
import psycopg2

database_url = 'postgresql+psycopg2://inform_user:inform@localhost:5433/fakeuds'
schema = "uds_schema"
search_path_preamble = f"set search_path to {schema};"
engine = sqlalchemy.create_engine(database_url)

# put in fixture
con = engine.connect()

qry = open("gaps.sql").read()

def run_with_params(visit_observation_type_id, source_location):
    params = (visit_observation_type_id, source_location)
    print(f"running with {params}")
    waveform_df = pd.read_sql_query(search_path_preamble + qry, con, params=params)
    return waveform_df

# --AND observation_datetime < %s

def test_all_for_gaps():
    all_params = pd.read_sql_query(search_path_preamble +
                                   """
                                   SELECT DISTINCT visit_observation_type_id, source_location
                                   FROM WAVEFORM
                                   """, con)
    print(all_params)
    print("!!!")
    for ps in all_params.itertuples():
        waveform_df = run_with_params(ps.visit_observation_type_id, ps.source_location)
        duration = pd.to_timedelta(waveform_df['values_array'].apply(len), "seconds") / waveform_df['sampling_rate']
        # duration = pd.Timedelta(seconds=len(waveform_df['values_array']) / waveform_df['sampling_rate'])
        waveform_df['duration'] = duration
        waveform_df['calc_end_date'] = waveform_df['observation_datetime'] + duration
        waveform_df['gap_since_last'] = (waveform_df['observation_datetime']
                                         - waveform_df['calc_end_date'].shift(1)).fillna(pd.Timedelta(0))
        first = waveform_df.iloc[0]
        last = waveform_df.iloc[-1]
        total_samples = waveform_df['values_array'].apply(len).sum()
        total_active_time = waveform_df['duration'].sum()
        total_calendar_time = last['calc_end_date'] - first['observation_datetime']
        # if there are no gaps or overlaps, total_active_time and total_calendar_time should be the same
        sampling_rate = waveform_df['sampling_rate'].unique().tolist()
        print(f"Total samples = {total_samples} @{sampling_rate}Hz, Total active time = {total_active_time}, total calendar = {total_calendar_time}")
        indexes_with_gap = waveform_df[waveform_df['gap_since_last'].apply(abs) > pd.Timedelta(milliseconds=1)].index
        print(f"Indexes with gap: {indexes_with_gap}")
        assert indexes_with_gap.empty;
        assert abs(total_active_time - total_calendar_time) < pd.Timedelta(milliseconds=1)

        # Index(['waveform_id', 'stored_from', 'valid_from', 'observation_datetime',
        #        'sampling_rate', 'source_location', 'unit', 'values_array',
        #        'location_visit_id', 'visit_observation_type_id'],
        #       dtype='object')
