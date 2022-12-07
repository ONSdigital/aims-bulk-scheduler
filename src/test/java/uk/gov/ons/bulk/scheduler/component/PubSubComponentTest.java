package uk.gov.ons.bulk.scheduler.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;

import uk.gov.ons.bulk.scheduler.entities.Message;

@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@DirtiesContext
class PubSubComponentTest {
	
	@Autowired
	private PubSubTemplate template;

	@Test
	public void testPubSubProcessingFinished() throws Exception {

		ObjectMapper objectMapper = new ObjectMapper();
		Message expectedMsg = objectMapper.readValue(new File("src/test/resources/message.json"),
				Message.class);

		template.publish("bulk-scheduler-test", Files.readString(Path.of("src/test/resources/message.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("processing-finished-subscription-test", 1, false);
		Message actualMessage = objectMapper.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), Message.class);

		assertEquals(expectedMsg, actualMessage);
	}
}
