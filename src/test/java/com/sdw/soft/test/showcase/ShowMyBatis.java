package com.sdw.soft.test.showcase;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;


/**
 * @author Sonicery_D
 * @date 2014年11月30日
 * @version 1.0.0
 * @description
 **/
public class ShowMyBatis {

	public static void main(String[] args) throws IOException{
		InputStream is = ShowMyBatis.class.getClass().getResourceAsStream("mybatis-config.xml");
//		InputStream iss = Resources.getResourceAsStream("mybatis-config.xml");
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
		SqlSession session = sqlSessionFactory.openSession();
		
	}
}
