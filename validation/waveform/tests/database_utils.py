from functools import lru_cache

import pandas as pd
import sqlalchemy
import psycopg2

database_url = 'postgresql+psycopg2://inform_user:inform@localhost:5433/fakeuds'
schema = "uds_schema"
SET_SEARCH_PATH = f"set search_path to {schema};"
engine = sqlalchemy.create_engine(database_url)


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


@lru_cache
def get_data_single_stream(visit_observation_type_id, source_location, min_time=None, max_time=None):
    params = (visit_observation_type_id, source_location)
    con = engine.connect()
    query = SET_SEARCH_PATH + """
                             SELECT *
                             FROM WAVEFORM
                             WHERE visit_observation_type_id = %s AND source_location = %s
                             """
    if min_time is not None:
        query += """
                 AND observation_datetime >= %s
                 """
        params += (min_time,)
    if max_time is not None:
        query += """
                 AND observation_datetime <= %s
                 """
        params += (max_time,)
    query += """
             ORDER BY observation_datetime
             """
    # print(f"qry = {query}, params = {params}")
    data = pd.read_sql_query(query, con, params=params)
    return data
