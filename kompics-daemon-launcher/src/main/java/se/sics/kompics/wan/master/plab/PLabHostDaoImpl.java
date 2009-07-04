package se.sics.kompics.wan.master.plab;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class PLabHostDaoImpl implements PLabHostDao {

	private static final SessionFactory sessionFactory;
	static {
	          try {
	                     // Create the SessionFactory from hibernate.cfg.xml
	                     sessionFactory = new Configuration().configure()
	                                        .buildSessionFactory();
	          } catch (Throwable ex) {
	                     // Make sure you log the exception, as it might be swallowed
	                     System.err.println("Initial SessionFactory creation failed." + ex);
	                     throw new ExceptionInInitializerError(ex);
	          }
	}

	
	@Override
	public List<PLabHost> loadAllHosts() {
		Session session = sessionFactory.openSession();
		List<PLabHost> list = session.createQuery("From PLabHost").list();
		return list;

	}

	@Override
	public void store(List<PLabHost> listHosts) {

		Session session = sessionFactory.openSession();
		session.getTransaction().begin();
		for (PLabHost host : listHosts) {
		         session.save(host);
		}
		session.getTransaction().commit();

		

	}

}
