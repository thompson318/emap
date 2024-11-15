import pandas as pd


def reset_times_OLD(gr):
    gr['new_observation_datetime'] = gr.apply(
        lambda row:
        row.observation_datetime + pd.Timedelta(seconds=row.idx_within_group / row.sampling_rate),
        axis=1)
    return gr


def explode_values_array(df):
    def reset_times(gr):
        return gr.assign(observation_datetime=(
                gr.observation_datetime + gr.idx_within_group.apply(
            lambda idx: pd.Timedelta(seconds=idx/gr.iloc[0].sampling_rate)
        )))

    # %%
    # explode into one row per data point, and adjust the observation datetimes
    one_per_row = df.explode('values_array')
    one_per_row = one_per_row.rename(columns={'values_array': 'value'})
    one_per_row['idx_within_group'] = one_per_row.groupby(level=0).cumcount()
    return one_per_row.groupby(level=0).apply(reset_times).reset_index(drop=True)
