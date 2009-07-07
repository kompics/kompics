package se.sics.kompics.wan.master.plab;

import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

// extends HibernateDaoSupport 
public class PLabHostDaoImpl  implements PLabHostDao {

	 private SessionFactory sessionFactory = null;

	 private HibernateTemplate ht = null;
//	private static final SessionFactory sessionFactory;

//	static {
//		try { // Create the SessionFactory from hibernate.cfg.xml
//			sessionFactory = new Configuration().configure().buildSessionFactory();
//		} catch (Throwable ex) {
//			// Make sure you log the exception, as it might be swallowed
//			System.err.println("Initial SessionFactory creation failed." + ex);
//			throw new ExceptionInInitializerError(ex);
//		}
//	}

	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		ht = new HibernateTemplate(sessionFactory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PLabHost> loadAllHosts() {
		// Session session = sessionFactory.openSession();
		// List<PLabHost> list = session.createQuery("From PLabHost").list();

		List<PLabHost> list = (List<PLabHost>) ht.find("from PLabHost");
		return list;

	}

	@Override
	public void store(List<PLabHost> listHosts) {

		// Session session = sessionFactory.openSession();
		// session.getTransaction().begin();

		for (PLabHost host : listHosts) {
			// session.save(host);
			ht.save(host);
		}
		// session.getTransaction().commit();

	}

}
