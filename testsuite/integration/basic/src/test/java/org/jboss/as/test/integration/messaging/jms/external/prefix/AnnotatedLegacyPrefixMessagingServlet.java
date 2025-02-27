/*
 * Copyright 2021 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.messaging.jms.external.prefix;

import static org.jboss.as.test.integration.messaging.jms.external.prefix.ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase.QUEUE_NAME;
import static org.jboss.as.test.integration.messaging.jms.external.prefix.ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase.TOPIC_LOOKUP;
import static org.jboss.as.test.integration.messaging.jms.external.prefix.ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase.QUEUE_LOOKUP;
import static org.jboss.as.test.integration.messaging.jms.external.prefix.ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase.REMOTE_PCF;
import static org.jboss.as.test.integration.messaging.jms.external.prefix.ExternalJMSDestinationDefinitionLegacyPrefixMessagingDeploymentTestCase.TOPIC_NAME;

import java.io.IOException;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSDestinationDefinitions;
import jakarta.jms.JMSPasswordCredential;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.test.shared.TimeoutUtil;
/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@JMSDestinationDefinitions(
        value = {
            @JMSDestinationDefinition(
                    resourceAdapter = REMOTE_PCF,
                    name = QUEUE_LOOKUP,
                    interfaceName = "jakarta.jms.Queue",
                    destinationName = QUEUE_NAME
            ),
            @JMSDestinationDefinition(
                    resourceAdapter = REMOTE_PCF,
                    name = TOPIC_LOOKUP,
                    interfaceName = "jakarta.jms.Topic",
                    destinationName = TOPIC_NAME,
                    properties = {"enable-amq1-prefix=true"}
            )
        }
)
@WebServlet("/ClientMessagingDeploymentTestCase")
public class AnnotatedLegacyPrefixMessagingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Resource(lookup = QUEUE_LOOKUP)
    private Queue queue;

    @Resource(lookup = TOPIC_LOOKUP)
    private Topic topic;

    @Inject
    @JMSConnectionFactory("java:/JmsXA")
    @JMSPasswordCredential(userName = "guest", password = "guest")
    private JMSContext context;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean useTopic = req.getParameterMap().keySet().contains("topic");
        final Destination destination = useTopic ? topic : queue;
        final String text = req.getParameter("text");

        String reply = sendAndReceiveMessage(destination, text);
        resp.getWriter().write(reply);
    }

    private String sendAndReceiveMessage(Destination destination, String text) {
        Destination replyTo = context.createTemporaryQueue();

        try (JMSConsumer consumer = context.createConsumer(replyTo)) {
            context.createProducer().setJMSReplyTo(replyTo).send(destination, text);
            return consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
        }
    }
}
