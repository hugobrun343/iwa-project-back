"""
Seeder for User Service - creates users with languages and specialisations.
"""
import logging
import random
from datetime import datetime
from seeders.base_seeder import get_db_config, wait_for_db, get_connection

logger = logging.getLogger(__name__)

# Sample user data
SAMPLE_USERS = [
    {
        'username': 'nauroy',
        'first_name': 'Christophe',
        'last_name': 'Nauroy',
        'email': 'christophe.nauroy@gmail.com',
        'phone_number': '+33696969696',
        'location': 'Paris, France',
        'description': 'The best teacher of the world !'
    },
    {
        'username': 'alice_martin',
        'first_name': 'Alice',
        'last_name': 'Martin',
        'email': 'alice.martin@example.com',
        'phone_number': '+33612345678',
        'location': 'Paris, France',
        'description': 'Experienced caregiver with 5 years of experience in home care.'
    },
    {
        'username': 'bob_dupont',
        'first_name': 'Bob',
        'last_name': 'Dupont',
        'email': 'bob.dupont@example.com',
        'phone_number': '+33623456789',
        'location': 'Lyon, France',
        'description': 'Professional nurse specialized in medical care and medication management.'
    },
    {
        'username': 'charlie_bernard',
        'first_name': 'Charlie',
        'last_name': 'Bernard',
        'email': 'charlie.bernard@example.com',
        'phone_number': '+33634567890',
        'location': 'Marseille, France',
        'description': 'Compassionate companion offering meal preparation and transportation services.'
    },
    {
        'username': 'diana_petit',
        'first_name': 'Diana',
        'last_name': 'Petit',
        'email': 'diana.petit@example.com',
        'phone_number': '+33645678901',
        'location': 'Toulouse, France',
        'description': 'Housekeeping and personal care specialist with excellent references.'
    },
    {
        'username': 'emma_roux',
        'first_name': 'Emma',
        'last_name': 'Roux',
        'email': 'emma.roux@example.com',
        'phone_number': '+33656789012',
        'location': 'Nice, France',
        'description': 'Physical therapy assistant and nursing care provider.'
    },
    {
        'username': 'frank_moreau',
        'first_name': 'Frank',
        'last_name': 'Moreau',
        'email': 'frank.moreau@example.com',
        'phone_number': '+33667890123',
        'location': 'Bordeaux, France',
        'description': 'Retired professional offering companionship and home care services.'
    }
]

def seed_users():
    """Seed users into the User Service database."""
    db_config = get_db_config('user')
    
    if not wait_for_db(db_config):
        logger.error("Failed to connect to user database")
        return False
    
    # Wait for tables to exist
    from seeders.base_seeder import wait_for_table
    if not wait_for_table(db_config, 'users'):
        logger.error("Table 'users' does not exist")
        return False
    if not wait_for_table(db_config, 'languages'):
        logger.error("Table 'languages' does not exist")
        return False
    if not wait_for_table(db_config, 'specialisations'):
        logger.error("Table 'specialisations' does not exist")
        return False
    
    try:
        conn = get_connection(db_config)
        cur = conn.cursor()
        
        # Check if users already exist
        cur.execute("SELECT COUNT(*) FROM users")
        user_count = cur.fetchone()[0]
        
        if user_count > 0:
            logger.info(f"User database already contains {user_count} users, skipping seeding")
            cur.close()
            conn.close()
            return True
        
        logger.info("Seeding users...")
        
        # Get available languages and specialisations
        cur.execute("SELECT label FROM languages")
        languages = [row[0] for row in cur.fetchall()]
        
        cur.execute("SELECT label FROM specialisations")
        specialisations = [row[0] for row in cur.fetchall()]
        
        if not languages or not specialisations:
            logger.warning("Languages or specialisations not found. Please seed them first.")
            cur.close()
            conn.close()
            return False
        
        # Insert users
        registration_date = datetime.now()
        inserted_users = []
        
        for user_data in SAMPLE_USERS:
            cur.execute("""
                INSERT INTO users (username, first_name, last_name, email, phone_number, 
                                 location, description, identity_verification, 
                                 registration_date)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (username) DO NOTHING
                RETURNING username
            """, (
                user_data['username'],
                user_data['first_name'],
                user_data['last_name'],
                user_data['email'],
                user_data['phone_number'],
                user_data['location'],
                user_data['description'],
                random.choice([True, False]),  # Random identity verification
                registration_date
            ))
            
            result = cur.fetchone()
            if result:
                inserted_users.append(result[0])
                
                # Assign random languages (1-3 per user)
                user_languages = random.sample(languages, k=random.randint(1, min(3, len(languages))))
                for lang in user_languages:
                    cur.execute("""
                        INSERT INTO user_languages (username, language_label)
                        VALUES (%s, %s)
                        ON CONFLICT DO NOTHING
                    """, (user_data['username'], lang))
                
                # Assign random specialisations (1-3 per user)
                user_specs = random.sample(specialisations, k=random.randint(1, min(3, len(specialisations))))
                for spec in user_specs:
                    cur.execute("""
                        INSERT INTO user_specialisations (username, specialisation_label)
                        VALUES (%s, %s)
                        ON CONFLICT DO NOTHING
                    """, (user_data['username'], spec))
        
        conn.commit()
        cur.close()
        conn.close()
        
        logger.info(f"✅ Seeded {len(inserted_users)} users with languages and specialisations!")
        return True
        
    except Exception as e:
        logger.error(f"Error seeding users: {e}")
        import traceback
        traceback.print_exc()
        return False

