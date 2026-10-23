# db/Dockerfile (the Dash dashboard), with the instruments ETL, schema and tests that
# docker-compose bind-mounts into the python container baked in instead.
# Built from the repo root: docker build -f tofu/images/python/Dockerfile .
FROM python:3.14

WORKDIR /db

COPY db/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY db/ .
COPY app/instruments-service/db/etl    ./etl
COPY app/instruments-service/db/schema ./schema
COPY app/instruments-service/db/tests  ./tests

CMD ["python", "dashboard/dashboard_app.py"]
