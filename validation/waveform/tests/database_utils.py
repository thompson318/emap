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
