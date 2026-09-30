FROM python:3.14

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY etl/ etl/
COPY tests/ tests/


CMD ["python", "etl/run_scheduler.py"]
