#!/usr/bin/env python3
"""
External seeder service for IWA Project databases.
Runs automatically when Docker Compose starts.
"""
import sys
import logging
from psycopg2.extras import execute_values

# Import seeders
from seeders.base_seeder import get_db_config, wait_for_db, get_connection
from seeders.user_seeder import seed_users
from seeders.announcement_seeder import seed_announcements
from seeders.application_seeder import seed_applications
from seeders.message_seeder import seed_discussions_and_messages

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def seed_reference_data():
    """Seed reference data (languages, specialisations, care types)."""
    success = True
    
    # Seed User Service reference data
    user_db_config = get_db_config('user')
    if not wait_for_db(user_db_config):
        logger.error("Failed to connect to user database")
        return False
    
    try:
        # Wait for tables to exist (created by Spring Boot)
        from seeders.base_seeder import wait_for_table
        if not wait_for_table(user_db_config, 'languages'):
            logger.error("Table 'languages' does not exist")
            return False
        if not wait_for_table(user_db_config, 'specialisations'):
            logger.error("Table 'specialisations' does not exist")
            return False
        
        conn = get_connection(user_db_config)
        cur = conn.cursor()
        
        # Check if data already exists
        cur.execute("SELECT COUNT(*) FROM languages")
        language_count = cur.fetchone()[0]
        
        cur.execute("SELECT COUNT(*) FROM specialisations")
        specialisation_count = cur.fetchone()[0]
        
        if language_count == 0 or specialisation_count == 0:
            logger.info("Seeding User Service reference data...")
            
            # Seed Languages
            languages = [
                "English", "French", "Spanish", "German", "Italian",
                "Portuguese", "Russian", "Chinese", "Japanese", "Arabic"
            ]
            
            language_values = [(lang,) for lang in languages]
            execute_values(
                cur,
                "INSERT INTO languages (label) VALUES %s ON CONFLICT (label) DO NOTHING",
                language_values
            )
            logger.info(f"Seeded {len(languages)} languages")
            
            # Seed Specialisations
            specialisations = [
                "Plumber", "Electrician", "Carpenter", "Painter", "Gardener",
                "Cleaner", "HVAC Technician", "Roofer", "Locksmith", "Handyman",
                "Interior Designer", "Landscaper", "Pool Maintenance",
                "Appliance Repair", "Flooring Specialist"
            ]
            
            spec_values = [(spec,) for spec in specialisations]
            execute_values(
                cur,
                "INSERT INTO specialisations (label) VALUES %s ON CONFLICT (label) DO NOTHING",
                spec_values
            )
            logger.info(f"Seeded {len(specialisations)} specialisations")
            
            conn.commit()
        else:
            logger.info(f"User reference data already exists ({language_count} languages, "
                       f"{specialisation_count} specialisations)")
        
        cur.close()
        conn.close()
        
    except Exception as e:
        logger.error(f"Error seeding user reference data: {e}")
        import traceback
        traceback.print_exc()
        success = False
    
    # Seed Announcement Service reference data
    announcement_db_config = get_db_config('announcement')
    if not wait_for_db(announcement_db_config):
        logger.error("Failed to connect to announcement database")
        return False
    
    try:
        # Wait for table to exist (created by Spring Boot)
        from seeders.base_seeder import wait_for_table
        if not wait_for_table(announcement_db_config, 'care_types'):
            logger.error("Table 'care_types' does not exist")
            return False
        
        conn = get_connection(announcement_db_config)
        cur = conn.cursor()
        
        # Check if data already exists
        cur.execute("SELECT COUNT(*) FROM care_types")
        count = cur.fetchone()[0]
        
        if count == 0:
            logger.info("Seeding Announcement Service reference data...")
            
            # Seed Care Types
            care_types = [
                "Home Care", "Medical Care", "Companionship", "Meal Preparation",
                "Transportation", "Housekeeping", "Personal Care",
                "Medication Management", "Physical Therapy", "Nursing Care"
            ]
            
            care_type_values = [(ct,) for ct in care_types]
            execute_values(
                cur,
                "INSERT INTO care_types (label) VALUES %s ON CONFLICT (label) DO NOTHING",
                care_type_values
            )
            logger.info(f"Seeded {len(care_types)} care types")
            
            conn.commit()
        else:
            logger.info(f"Announcement reference data already exists ({count} care types)")
        
        cur.close()
        conn.close()
        
    except Exception as e:
        logger.error(f"Error seeding announcement reference data: {e}")
        import traceback
        traceback.print_exc()
        success = False
    
    return success

def get_user_usernames():
    """Get list of user usernames from database."""
    user_db_config = get_db_config('user')
    try:
        conn = get_connection(user_db_config)
        cur = conn.cursor()
        cur.execute("SELECT username FROM users")
        usernames = [row[0] for row in cur.fetchall()]
        cur.close()
        conn.close()
        return usernames
    except Exception as e:
        logger.error(f"Error getting user usernames: {e}")
        return []

def main():
    """Main seeder function."""
    logger.info("🚀 Starting external seeder service...")
    
    success = True
    
    # Step 1: Seed reference data (languages, specialisations, care types)
    logger.info("📋 Step 1: Seeding reference data...")
    if not seed_reference_data():
        success = False
    
    # Step 2: Seed users
    logger.info("👥 Step 2: Seeding users...")
    if not seed_users():
        success = False
    
    # Step 3: Get user usernames for subsequent seeders
    user_usernames = get_user_usernames()
    if not user_usernames:
        logger.warning("No users found, skipping dependent seeders")
        if success:
            logger.info("✅ Reference data seeded successfully!")
        sys.exit(0 if success else 1)
    
    # Step 4: Seed announcements
    logger.info("📢 Step 3: Seeding announcements...")
    announcement_ids = seed_announcements(user_usernames)
    if not announcement_ids:
        logger.warning("No announcements created")
    
    # Step 5: Seed applications
    logger.info("📝 Step 4: Seeding applications...")
    if announcement_ids:
        if not seed_applications(announcement_ids, user_usernames):
            success = False
    else:
        logger.warning("Skipping applications seeding (no announcements)")
    
    # Step 6: Seed discussions and messages
    logger.info("💬 Step 5: Seeding discussions and messages...")
    if announcement_ids:
        if not seed_discussions_and_messages(announcement_ids, user_usernames):
            success = False
    else:
        logger.warning("Skipping messages seeding (no announcements)")
    
    if success:
        logger.info("✅ All databases seeded successfully!")
        sys.exit(0)
    else:
        logger.error("❌ Some databases failed to seed")
        sys.exit(1)

if __name__ == "__main__":
    main()
