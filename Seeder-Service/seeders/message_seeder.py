"""
Seeder for Chat Service - creates discussions and messages.
"""
import logging
import random
from datetime import datetime, timedelta
from seeders.base_seeder import get_db_config, wait_for_db, get_connection

logger = logging.getLogger(__name__)

# Sample message templates
MESSAGE_TEMPLATES = [
    "Bonjour, je suis intéressé(e) par votre annonce. Pourriez-vous me donner plus de détails ?",
    "J'ai de l'expérience dans ce domaine et je serais ravi(e) de vous aider.",
    "Quelles sont les horaires exacts requis pour cette mission ?",
    "Merci pour votre réponse. Je suis disponible pour un entretien si vous le souhaitez.",
    "Parfait, cela me convient. Quand pouvons-nous commencer ?",
    "J'ai quelques questions concernant la rémunération et les conditions.",
    "D'accord, je comprends. Je suis prêt(e) à commencer dès que possible.",
    "Merci beaucoup pour cette opportunité !",
    "Pourriez-vous me donner plus d'informations sur les tâches à effectuer ?",
    "Je suis disponible pour discuter de cette annonce en détail."
]

def seed_discussions_and_messages(announcement_ids, user_usernames):
    """Seed discussions and messages into the Chat Service database."""
    db_config = get_db_config('chat')
    
    if not wait_for_db(db_config):
        logger.error("Failed to connect to chat database")
        return False
    
    # Wait for tables to exist
    from seeders.base_seeder import wait_for_table
    if not wait_for_table(db_config, 'discussions'):
        logger.error("Table 'discussions' does not exist")
        return False
    if not wait_for_table(db_config, 'messages'):
        logger.error("Table 'messages' does not exist")
        return False
    
    try:
        conn = get_connection(db_config)
        cur = conn.cursor()
        
        # Check if discussions already exist
        cur.execute("SELECT COUNT(*) FROM discussions")
        discussion_count = cur.fetchone()[0]
        
        if discussion_count > 0:
            logger.info(f"Chat database already contains {discussion_count} discussions, skipping seeding")
            cur.close()
            conn.close()
            return True
        
        if not announcement_ids or len(user_usernames) < 2:
            logger.warning("Not enough announcements or users for seeding discussions")
            cur.close()
            conn.close()
            return True
        
        logger.info("Seeding discussions and messages...")
        
        # Create discussions - link some announcements to conversations
        # Each discussion is between a sender and recipient about an announcement
        inserted_discussions = []
        now = datetime.now()
        
        # Create discussions for some announcements (not all)
        selected_announcements = random.sample(
            announcement_ids, 
            k=min(len(announcement_ids), len(announcement_ids) // 2 + 1)
        )
        
        for announcement_id in selected_announcements:
            # Pick two different users for the discussion
            sender, recipient = random.sample(user_usernames, k=2)
            
            # Create discussion
            cur.execute("""
                INSERT INTO discussions (annonce_id, expediteur_id, destinataire_id, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s)
                RETURNING id
            """, (
                announcement_id,
                sender,
                recipient,
                now,
                now
            ))
            
            result = cur.fetchone()
            if result:
                discussion_id = result[0]
                inserted_discussions.append(discussion_id)
                
                # Create 2-5 messages in each discussion
                num_messages = random.randint(2, 5)
                message_time = now - timedelta(hours=num_messages)
                
                for i in range(num_messages):
                    # Alternate between sender and recipient
                    author = sender if i % 2 == 0 else recipient
                    message_time += timedelta(minutes=random.randint(5, 60))
                    
                    # Select random message template
                    content = random.choice(MESSAGE_TEMPLATES)
                    
                    cur.execute("""
                        INSERT INTO messages (discussion_id, auteur_id, contenu, created_at)
                        VALUES (%s, %s, %s, %s)
                    """, (
                        discussion_id,
                        author,
                        content,
                        message_time
                    ))
        
        conn.commit()
        cur.close()
        conn.close()
        
        logger.info(f"✅ Seeded {len(inserted_discussions)} discussions with messages!")
        return True
        
    except Exception as e:
        logger.error(f"Error seeding discussions and messages: {e}")
        import traceback
        traceback.print_exc()
        return False

