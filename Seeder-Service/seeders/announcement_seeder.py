"""
Seeder for Announcement Service - creates announcements.
"""
import logging
import random
from datetime import datetime, timedelta
from seeders.base_seeder import get_db_config, wait_for_db, get_connection

logger = logging.getLogger(__name__)

# Sample announcement data
ANNOUNCEMENT_TEMPLATES = [
    {
        'title': 'Garde de chat pendant les vacances',
        'description': 'Recherche une personne de confiance pour s\'occuper de mon chat pendant mes vacances. Nourriture et câlins quotidiens requis.',
        'visit_frequency': 'Quotidien',
        'remuneration': 25.0
    },
    {
        'title': 'Accompagnement médical hebdomadaire',
        'description': 'Besoin d\'accompagnement pour rendez-vous médicaux hebdomadaires. Transport et assistance requis.',
        'visit_frequency': 'Hebdomadaire',
        'remuneration': 50.0
    },
    {
        'title': 'Préparation de repas pour personne âgée',
        'description': 'Recherche aide pour préparation de repas équilibrés pour une personne âgée. 3 fois par semaine.',
        'visit_frequency': '3 fois par semaine',
        'remuneration': 30.0
    },
    {
        'title': 'Soins à domicile urgents',
        'description': 'Besoin urgent de soins à domicile suite à une opération. Assistance personnelle et médicale requise.',
        'visit_frequency': 'Quotidien',
        'remuneration': 60.0,
        'urgent': True
    },
    {
        'title': 'Ménage et entretien régulier',
        'description': 'Recherche aide ménagère pour entretien régulier d\'un appartement. Tâches ménagères et courses.',
        'visit_frequency': '2 fois par semaine',
        'remuneration': 35.0
    },
    {
        'title': 'Companionship et activités',
        'description': 'Recherche compagnon pour activités et sorties. Personne agréable et patiente recherchée.',
        'visit_frequency': '2 fois par semaine',
        'remuneration': 20.0
    },
    {
        'title': 'Gestion de médicaments',
        'description': 'Besoin d\'aide pour gestion quotidienne des médicaments et suivi médical.',
        'visit_frequency': 'Quotidien',
        'remuneration': 40.0
    },
    {
        'title': 'Thérapie physique à domicile',
        'description': 'Recherche assistant pour exercices de thérapie physique à domicile. Expérience requise.',
        'visit_frequency': '3 fois par semaine',
        'remuneration': 55.0
    }
]

LOCATIONS = [
    'Paris, France',
    'Lyon, France',
    'Marseille, France',
    'Toulouse, France',
    'Nice, France',
    'Bordeaux, France'
]

def seed_announcements(user_usernames):
    """Seed announcements into the Announcement Service database."""
    db_config = get_db_config('announcement')
    
    if not wait_for_db(db_config):
        logger.error("Failed to connect to announcement database")
        return []
    
    # Wait for tables to exist
    from seeders.base_seeder import wait_for_table
    if not wait_for_table(db_config, 'announcements'):
        logger.error("Table 'announcements' does not exist")
        return []
    if not wait_for_table(db_config, 'care_types'):
        logger.error("Table 'care_types' does not exist")
        return []
    
    try:
        conn = get_connection(db_config)
        cur = conn.cursor()
        
        # Check if announcements already exist
        cur.execute("SELECT COUNT(*) FROM announcements")
        announcement_count = cur.fetchone()[0]
        
        if announcement_count > 0:
            logger.info(f"Announcement database already contains {announcement_count} announcements, skipping seeding")
            cur.close()
            conn.close()
            return []
        
        # Get care types
        cur.execute("SELECT id FROM care_types")
        care_type_ids = [row[0] for row in cur.fetchall()]
        
        if not care_type_ids:
            logger.warning("Care types not found. Please seed them first.")
            cur.close()
            conn.close()
            return []
        
        logger.info("Seeding announcements...")
        
        # Create announcements
        inserted_announcements = []
        now = datetime.now()
        
        for i, template in enumerate(ANNOUNCEMENT_TEMPLATES):
            # Assign to random user
            owner_username = random.choice(user_usernames) if user_usernames else 'alice_martin'
            
            # Random dates (start in future, end 1-4 weeks later)
            start_date = now.date() + timedelta(days=random.randint(1, 14))
            end_date = start_date + timedelta(days=random.randint(7, 28))
            
            # Random care type
            care_type_id = random.choice(care_type_ids)
            
            # Random location
            location = random.choice(LOCATIONS)
            
            cur.execute("""
                INSERT INTO announcements (
                    owner_username, title, location, description, 
                    specific_instructions, care_type_id, start_date, end_date,
                    visit_frequency, remuneration, identity_verification_required,
                    urgent_request, status, creation_date
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                RETURNING id
            """, (
                owner_username,
                template['title'],
                location,
                template['description'],
                f"Instructions spécifiques pour {template['title']}",
                care_type_id,
                start_date,
                end_date,
                template['visit_frequency'],
                template['remuneration'],
                random.choice([True, False]),
                template.get('urgent', False),
                'PUBLISHED',
                now
            ))
            
            result = cur.fetchone()
            if result:
                inserted_announcements.append(result[0])
        
        conn.commit()
        cur.close()
        conn.close()
        
        logger.info(f"✅ Seeded {len(inserted_announcements)} announcements!")
        return inserted_announcements
        
    except Exception as e:
        logger.error(f"Error seeding announcements: {e}")
        import traceback
        traceback.print_exc()
        return []

