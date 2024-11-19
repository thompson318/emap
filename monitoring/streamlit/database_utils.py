import math
import os
from datetime import timedelta
from functools import lru_cache

import pandas as pd
import sqlalchemy
from sqlalchemy.engine.url import make_url
import psycopg2

# Perhaps we should move away from making the JDBC url primary, but
# for now we will have to accept this and make some edits so we can
# use it here.
database_jdbc_url = os.environ['UDS_JDBC_URL']
database_user = os.environ['UDS_USERNAME']
database_password = os.environ['UDS_PASSWORD']
database_schema = os.environ['UDS_SCHEMA']
database_url = make_url(database_jdbc_url.replace("jdbc:", ""))
# host, database, and port will be correct, but change the driver and user/pass
database_url = database_url.set(drivername='postgresql+psycopg2', username=database_user, password=database_password)

SET_SEARCH_PATH = f"set search_path to {database_schema};"
engine = sqlalchemy.create_engine(database_url)

@lru_cache
def get_all_params():
    con = engine.connect()
    return pd.read_sql_query(SET_SEARCH_PATH +
                             """
                             SELECT DISTINCT visit_observation_type_id, source_location
                             FROM WAVEFORM
                             """, con)


@lru_cache
def get_min_max_time_for_single_stream(visit_observation_type_id, source_location):
    params = (visit_observation_type_id, source_location)
    con = engine.connect()
    query = SET_SEARCH_PATH + """
                             SELECT min(observation_datetime) as min_time, max(observation_datetime) as max_time
                             FROM WAVEFORM
                             WHERE visit_observation_type_id = %s AND source_location = %s
                             """
    minmax = pd.read_sql_query(query, con, params=params)
    if minmax.empty:
        return None, None
    else:
        return minmax.iloc[0].min_time, minmax.iloc[0].max_time


def get_data_single_stream_rounded(visit_observation_type_id, source_location, min_time, max_time, max_row_length_seconds=30):
    # Because a row's observation_datetime is the time of the *first* data point in the array,
    # to get the data starting at time T, you have to query the DB for data a little earlier than T.
    # Additionally, to aid caching, round down further so repeated calls with
    # approximately similar values of min_time will result in exactly the
    # same query being issued (which is hopefully already cached)
    actual_min_time = min_time - timedelta(seconds=max_row_length_seconds)
    rounded_seconds = actual_min_time.second // 10 * 10
    rounded_min_time = actual_min_time.replace(second=rounded_seconds, microsecond=0)
    # For the same reason, round the max value up to the nearest few secondsA (5 is pretty arbitrary)
    # (using +timedelta instead of replacing seconds value because you might hit 60 and have to wrap around)
    rounded_max_time = max_time.replace(second=0, microsecond=0) + timedelta(seconds=math.ceil(max_time.second / 5) * 5)
    print(f"Adjusted min time {min_time} -> {rounded_min_time}")
    print(f"Adjusted max time {max_time} -> {rounded_max_time}")
    return get_data_single_stream(visit_observation_type_id, source_location, rounded_min_time, rounded_max_time)


@lru_cache
def get_data_single_stream(visit_observation_type_id, source_location, min_time, max_time):
    params = (visit_observation_type_id, source_location, min_time, max_time)
    con = engine.connect()
    # Index(['waveform_id', 'stored_from', 'valid_from', 'observation_datetime',
    #        'sampling_rate', 'source_location', 'unit', 'values_array',
    #        'location_visit_id', 'visit_observation_type_id'],
    #       dtype='object')
    # It's much quicker to do the array unpacking and date calculation here rather than in pandas later.
    # This will still need a trim because the way the SQL arrays work you get more data than you need.
    query = SET_SEARCH_PATH + """
                             SELECT
                                 w.waveform_id,
                                 w.observation_datetime AS base_observation_datetime,
                                 w.observation_datetime + make_interval(secs => (v.ordinality - 1)::float / w.sampling_rate) AS observation_datetime,
                                 v.v as waveform_value,
                                 v.ordinality,
                                 w.sampling_rate,
                                 w.source_location,
                                 w.unit,
                                 w.location_visit_id,
                                 w.visit_observation_type_id
                             FROM WAVEFORM w, unnest(w.values_array) WITH ORDINALITY v
                             WHERE visit_observation_type_id = %s AND source_location = %s
                               AND observation_datetime >= %s
                               AND observation_datetime <= %s
                             ORDER BY observation_datetime
                             """
    # print(f"qry = {query}, params = {params}")
    data = pd.read_sql_query(query, con, params=params)
    return data
