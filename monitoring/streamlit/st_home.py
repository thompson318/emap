import streamlit as st
from st_waveform import waveform_data
from st_integrity import data_integrity
import database_utils

st.set_page_config(layout="wide")

# All pages
pages = {
    "Waveform Data": waveform_data,
    "Data integrity": data_integrity,
}

# sidebar
sb = st.sidebar
sb.title("Pages")
selection = sb.selectbox("Go to", list(pages.keys()))
sb.write(f"Schema: {database_utils.database_schema}")

# Render the selected page
page = pages[selection]
page()
