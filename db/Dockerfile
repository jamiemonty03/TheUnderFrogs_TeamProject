FROM python:3.14

WORKDIR /db

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["python", "dashboard/dashboard_app.py"]
