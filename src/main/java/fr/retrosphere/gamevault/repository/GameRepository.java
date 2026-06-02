package fr.retrosphere.gamevault.repository;

import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.persistence.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class GameRepository {
    public List<Game> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Game g order by g.addedAt desc", Game.class).list();
        }
    }

    public Optional<Game> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Game.class, id));
        }
    }

    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("select count(g) from Game g", Long.class).uniqueResult();
        }
    }

    public Game save(Game game) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Game merged = session.merge(game);
            transaction.commit();
            return merged;
        } catch (RuntimeException exception) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw exception;
        }
    }

    public void delete(Game game) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Game managed = session.get(Game.class, game.getId());
            if (managed != null) {
                session.remove(managed);
            }
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw exception;
        }
    }
}
