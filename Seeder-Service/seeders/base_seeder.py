"""
Base seeder utilities and database connection helpers.
"""
import os
import time
import psycopg2
import logging

logger = logging.getLogger(__name__)

def get_db_config(db_name):
    """Get database configuration from environment variables."""
    return {
        'host': os.getenv(f'{db_name.upper()}_DB_HOST'),
        'port': os.getenv(f'{db_name.upper()}_DB_PORT', '5432'),
        'database': os.getenv(f'{db_name.upper()}_DB_NAME'),
        'user': os.getenv(f'{db_name.upper()}_DB_USER'),
        'password': os.getenv(f'{db_name.upper()}_DB_PASSWORD')
    }

def wait_for_db(db_config, max_retries=30, retry_delay=2):
    """Wait for database to be ready."""
    logger.info(f"Waiting for database {db_config['database']}...")
    for i in range(max_retries):
        try:
            conn = psycopg2.connect(**db_config)
            conn.close()
            logger.info(f"Database {db_config['database']} is ready!")
            return True
        except psycopg2.OperationalError as e:
            if i < max_retries - 1:
                logger.info(f"Database not ready, retrying in {retry_delay}s... ({i+1}/{max_retries})")
                time.sleep(retry_delay)
            else:
                logger.error(f"Failed to connect to database {db_config['database']}: {e}")
                return False
    return False

def wait_for_table(db_config, table_name, max_retries=60, retry_delay=5):
    """Wait for a table to exist in the database."""
    logger.info(f"Waiting for table '{table_name}' to exist in {db_config['database']}...")
    for i in range(max_retries):
        try:
            conn = psycopg2.connect(**db_config)
            cur = conn.cursor()
            cur.execute("""
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_schema = 'public' 
                    AND table_name = %s
                )
            """, (table_name,))
            exists = cur.fetchone()[0]
            cur.close()
            conn.close()
            
            if exists:
                logger.info(f"Table '{table_name}' exists!")
                return True
            else:
                if i < max_retries - 1:
                    logger.info(f"Table '{table_name}' not found, waiting {retry_delay}s... ({i+1}/{max_retries})")
                    time.sleep(retry_delay)
                else:
                    logger.error(f"Table '{table_name}' does not exist after {max_retries} retries")
                    return False
        except Exception as e:
            if i < max_retries - 1:
                logger.info(f"Error checking table '{table_name}', retrying in {retry_delay}s... ({i+1}/{max_retries}): {e}")
                time.sleep(retry_delay)
            else:
                logger.error(f"Failed to check table '{table_name}': {e}")
                return False
    return False

def get_connection(db_config):
    """Get a database connection."""
    return psycopg2.connect(**db_config)

