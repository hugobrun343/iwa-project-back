FROM python:3.11-slim

WORKDIR /app

# Install PostgreSQL client libraries
RUN apt-get update && apt-get install -y \
    libpq-dev \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install dependencies
COPY Seeder-Service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy seeder script and modules
COPY Seeder-Service/seeder.py .
COPY Seeder-Service/seeders/ ./seeders/

# Run seeder
CMD ["python", "seeder.py"]

