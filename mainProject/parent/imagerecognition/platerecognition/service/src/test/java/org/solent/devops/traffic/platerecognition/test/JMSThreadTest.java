/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.solent.devops.traffic.platerecognition.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.activemq.command.ActiveMQTextMessage;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.solent.devops.message.jms.SimpleJmsListener;
import org.solent.devops.message.jms.SimpleJmsSender;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author 4GILLK91 <4GILLK91@solent.ac.uk>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/appconfig-service-test.xml"})
public class JMSThreadTest {
    
    @Autowired
    private SimpleJmsSender sender;

    @Test
    public void test() throws Exception {
        sender.send("Test.Input", "foo");
        TestConfig.latch.await(10, TimeUnit.SECONDS);
        ActiveMQTextMessage response = (ActiveMQTextMessage) TestConfig.received;
        assertEquals("foo",response.getText());
    }

    @Configuration
    public static class TestConfig {

        private static final CountDownLatch latch = new CountDownLatch(1);

        private static Object received;

        @Bean
        public static BeanPostProcessor listenerWrapper() {
            return new BeanPostProcessor() {

                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                    if (bean instanceof SimpleJmsListener) {
                        MethodInterceptor interceptor = new MethodInterceptor() {

                            @Override
                            public Object invoke(MethodInvocation invocation) throws Throwable {
                                Object result = invocation.proceed();
                                if (invocation.getMethod().getName().equals("onMessage")) {
                                    received = invocation.getArguments()[0];
                                    latch.countDown();
                                }
                                return result;
                            }

                        };
                        if (AopUtils.isAopProxy(bean)) {
                            ((Advised) bean).addAdvice(interceptor);
                            return bean;
                        }
                        else {
                            ProxyFactory proxyFactory = new ProxyFactory(bean);
                            proxyFactory.addAdvice(interceptor);
                            return proxyFactory.getProxy();
                        }
                    }
                    else {
                        return bean;
                    }
                }

            };
        }

    }

}