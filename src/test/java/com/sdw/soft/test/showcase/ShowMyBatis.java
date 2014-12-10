package com.sdw.soft.test.showcase;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Sonicery_D
 * @date 2014年11月30日
 * @version 1.0.0
 * @description
 **/
public class ShowMyBatis {

	private static final Logger logger = LoggerFactory.getLogger(ShowMyBatis.class);
	private SqlSession session = null;
	@Before
	public void setup() throws IOException{
		InputStream is = Resources.getResourceAsStream("com/sdw/soft/test/showcase/mybatis-config.xml");
//		InputStream is = ShowMyBatis.class.getClass().getResourceAsStream("/com/sdw/soft/test/showcase/mybatis-config.xml");
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
		if(session == null)
			session = sqlSessionFactory.openSession();
	}
	/**
	 * 插入测试
	 * @throws IOException
	 */
	@Test
	public void test01() throws IOException{

		User user = new User(UUID.randomUUID().toString(),"Sonicery_D","admin",23,"beijing",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		session.insert("addUser", user);
		session.commit();
		session.close();
		logger.info("------------执行完毕----------");
	}
	
	@Test
	public void test02(){
		List<User> users = session.selectList("fetchUser");
		if(users.size() > 0){
			User user = users.get(0);
			logger.debug("id->{}",user.getId());
			logger.debug("username->{}",user.getUsername());
			logger.debug("password->{}",user.getPassword());
			logger.debug("age->{}",user.getAge());
			logger.debug("address->{}",user.getAddress());
			logger.debug("create_date->{}",user.getCreate_date());
		}
		session.close();
		logger.info("------------执行完毕----------");
	}
	
	@Test
	public void test03(){
		User user = new User("476c2bec-3ee6-43a1-942a-0e20359e5f40","sdw2330976","root",25,"xinyang",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		session.update("modifyUser", user);
		session.commit();
		session.close();
		logger.info("------------执行完毕----------");
	}
	
	@Test
	public void test04(){
		session.delete("deleteUser","476c2bec-3ee6-43a1-942a-0e20359e5f40");
		session.commit();
		session.close();
		logger.info("------------执行完毕----------");
	}
}
