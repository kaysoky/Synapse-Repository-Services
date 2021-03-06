package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOComment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageRecipient;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUser;
import org.sagebionetworks.repo.model.message.Comment;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;

public class MessageUtilsTest {
	
	@Test
	public void testMessageRecipientBucketing() throws Exception {
		DBOMessageRecipient r1 = new DBOMessageRecipient();
		r1.setMessageId(1L);
		r1.setRecipientId(1L);
		
		DBOMessageRecipient r2 = new DBOMessageRecipient();
		r2.setMessageId(1L);
		r2.setRecipientId(2L);
		
		DBOMessageRecipient r3 = new DBOMessageRecipient();
		r3.setMessageId(2L);
		r3.setRecipientId(3L);
		
		List<DBOMessageRecipient> recipients = new ArrayList<DBOMessageRecipient>();
		recipients.add(r1);
		recipients.add(r2);
		recipients.add(r3);

		MessageToUser b1 = new MessageToUser();
		b1.setId("1");
		
		MessageToUser b2 = new MessageToUser();
		b2.setId("2");
		
		MessageToUser b3 = new MessageToUser();
		b3.setId("3");
		
		List<MessageToUser> buckets = new ArrayList<MessageToUser>();
		buckets.add(b1);
		buckets.add(b2);
		buckets.add(b3);
		
		MessageUtils.copyDBOToDTO(recipients, buckets);
		assertEquals(2, b1.getRecipients().size());
		assertEquals(1, b2.getRecipients().size());
		assertEquals(0, b3.getRecipients().size());
	}
	
	@Test
	public void testCommentAggregateRoundTrip() throws Exception {
		Comment dto = new Comment();
		dto.setId("1");
		dto.setCreatedBy("2");
		dto.setFileHandleId("3");
		dto.setCreatedOn(new Date());
		dto.setTargetId("456");
		dto.setTargetType(ObjectType.ENTITY);

		DBOMessageContent content = new DBOMessageContent();
		DBOComment info = new DBOComment();
		MessageUtils.copyDTOtoDBO(dto, content, info);

		Comment dto2 = new Comment();
		MessageUtils.copyDBOToDTO(content, info, dto2);
		assertEquals(dto, dto2);
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testMessageAggregateRoundTrip() throws Exception {
		MessageToUser dto = new MessageToUser();
		dto.setId("1");
		dto.setCreatedBy("2");
		dto.setFileHandleId("3");
		dto.setInReplyTo("4");
		dto.setInReplyToRoot("5");
		dto.setCreatedOn(new Date());
		dto.setSubject("foo");
		dto.setRecipients(new HashSet<String>() {{
			add("1");
			add("2");
			add("3");
			add("4");
			add("5");
		}});

		DBOMessageContent content = new DBOMessageContent();
		DBOMessageToUser info = new DBOMessageToUser();
		List<DBOMessageRecipient> recipients = new ArrayList<DBOMessageRecipient>();
		MessageUtils.copyDTOtoDBO(dto, content, info, recipients);

		MessageToUser dto2 = new MessageToUser();
		MessageUtils.copyDBOToDTO(content, info, recipients, dto2);
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testCommentInfoRoundTrip() throws Exception {
		Comment dto = new Comment();
		dto.setId("123");
		dto.setTargetId("456");
		dto.setTargetType(ObjectType.ENTITY);

		DBOComment info = new DBOComment();
		MessageUtils.copyDTOToDBO(dto, info);

		Comment dto2 = new Comment();
		MessageUtils.copyDBOToDTO(info, dto2);
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testMessageContentRoundTrip() throws Exception {
		MessageToUser dto = new MessageToUser();
		dto.setId("123");
		dto.setCreatedBy("456");
		dto.setFileHandleId("789");
		dto.setCreatedOn(new Date());
		
		DBOMessageContent content = new DBOMessageContent();
		MessageUtils.copyDTOToDBO(dto, content);

		MessageToUser dto2 = new MessageToUser();
		MessageUtils.copyDBOToDTO(content, dto2);
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testMessageInfoRoundTrip() throws Exception {
		MessageToUser dto = new MessageToUser();
		dto.setId("123");
		dto.setInReplyTo("456");
		dto.setInReplyToRoot("789");
		dto.setSubject("foo");
		
		DBOMessageToUser info = new DBOMessageToUser();
		MessageUtils.copyDTOToDBO(dto, info);

		MessageToUser dto2 = new MessageToUser();
		MessageUtils.copyDBOToDTO(info, dto2);
		assertEquals(dto, dto2);
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testMessageRecipientRoundTrip() throws Exception {
		MessageToUser dto = new MessageToUser();
		dto.setId("12345");
		dto.setRecipients(new HashSet<String>() {{
			add("1");
			add("2");
			add("3");
			add("4");
			add("5");
		}});
		
		List<DBOMessageRecipient> recipients = new ArrayList<DBOMessageRecipient>();
		MessageUtils.copyDTOToDBO(dto, recipients);
		
		MessageToUser dto2 = new MessageToUser();
		MessageUtils.copyDBOToDTO(recipients, dto2);
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testMessageStatusRoundTrip() throws Exception {
		MessageStatus dto = new MessageStatus();
		dto.setMessageId("-1");
		dto.setRecipientId("-1");
		dto.setStatus(MessageStatusType.UNREAD);
		
		DBOMessageStatus dbo = MessageUtils.convertDTO(dto);
		MessageStatus dto2 = MessageUtils.convertDBO(dbo);
		
		assertEquals(dto, dto2);
	}
}
