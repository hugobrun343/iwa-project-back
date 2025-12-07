"""
Seeder for Application Service - creates applications (reservations).
"""
import logging
import random
from datetime import datetime
from seeders.base_seeder import get_db_config, wait_for_db, get_connection

logger = logging.getLogger(__name__)

# Application statuses
STATUSES = ['SENT', 'ACCEPTED', 'REFUSED']

def seed_applications(announcement_ids, guardian_usernames):
    """Seed applications into the Application Service database."""
    db_config = get_db_config('application')
    
    if not wait_for_db(db_config):
        logger.error("Failed to connect to application database")
        return False
    
    # Wait for table to exist
    from seeders.base_seeder import wait_for_table
    if not wait_for_table(db_config, 'applications'):
        logger.error("Table 'applications' does not exist")
        return False
    
    try:
        conn = get_connection(db_config)
        cur = conn.cursor()
        
        # Check if applications already exist
        cur.execute("SELECT COUNT(*) FROM applications")
        application_count = cur.fetchone()[0]
        
        if application_count > 0:
            logger.info(f"Application database already contains {application_count} applications, skipping seeding")
            cur.close()
            conn.close()
            return True
        
        if not announcement_ids or not guardian_usernames:
            logger.warning("No announcements or guardians available for seeding applications")
            cur.close()
            conn.close()
            return True
        
        logger.info("Seeding applications...")
        
        # Create applications - each announcement gets 1-3 applications
        inserted_count = 0
        application_date = datetime.now()
        
        for announcement_id in announcement_ids:
            # Random number of applications per announcement (1-3)
            num_applications = random.randint(1, min(3, len(guardian_usernames)))
            
            # Select random guardians (avoid duplicates for same announcement)
            selected_guardians = random.sample(guardian_usernames, k=num_applications)
            
            for guardian_username in selected_guardians:
                # Random status (mostly SENT, some ACCEPTED/REFUSED)
                status = random.choices(
                    STATUSES,
                    weights=[70, 20, 10]  # 70% SENT, 20% ACCEPTED, 10% REFUSED
                )[0]
                
                cur.execute("""
                    INSERT INTO applications (annonce_id, guardian_username, status, date_candidature)
                    VALUES (%s, %s, %s, %s)
                    ON CONFLICT DO NOTHING
                """, (
                    announcement_id,
                    guardian_username,
                    status,
                    application_date
                ))
                
                if cur.rowcount > 0:
                    inserted_count += 1
        
        conn.commit()
        cur.close()
        conn.close()
        
        logger.info(f"✅ Seeded {inserted_count} applications!")
        return True
        
    except Exception as e:
        logger.error(f"Error seeding applications: {e}")
        import traceback
        traceback.print_exc()
        return False

