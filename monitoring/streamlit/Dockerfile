FROM python:3.12-slim-bullseye
WORKDIR /app/streamlit
COPY monitoring/requirements.txt /app/streamlit
RUN pip install -r requirements.txt
COPY monitoring/streamlit/ /app/streamlit
CMD streamlit run \
    --browser.gatherUsageStats=false \
    --server.enableWebsocketCompression=false \
    --server.enableXsrfProtection=false \
    # base URL to match where the proxy expects it to be - simpler than URL rewriting in the proxy
    --server.baseUrlPath "streamlit" \
    # Without this, websocket calls don't work behind nginx
    --browser.serverAddress ${SERVER_EXTERNAL_HOSTNAME} \
    --browser.serverPort ${PORTAL_PORT} \
    st_home.py
