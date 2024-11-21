FROM python:3.12-slim-bullseye
WORKDIR /app/streamlit
COPY monitoring/requirements.txt /app/streamlit
RUN pip install -r requirements.txt
COPY monitoring/streamlit/ /app/streamlit
CMD streamlit run --browser.gatherUsageStats=false --server.baseUrlPath "streamlit" st_app.py
