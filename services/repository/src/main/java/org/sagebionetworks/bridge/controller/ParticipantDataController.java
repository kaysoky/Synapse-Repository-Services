package org.sagebionetworks.bridge.controller;

import org.sagebionetworks.bridge.BridgeUrlHelpers;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.service.BridgeServiceProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.table.PaginatedRowSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

// @ControllerInfo(displayName = "Participant data", path = BridgeUrlHelpers.BASE_V1)
@Controller
public class ParticipantDataController {

	@Autowired
	BridgeServiceProvider serviceProvider;

	/**
	 * Append new participant data rows
	 * 
	 * @param userId
	 * @param participantDataId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_ID, method = RequestMethod.POST)
	public @ResponseBody
	RowSet appendParticipantData(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@PathVariable String participantDataId, @RequestBody RowSet data) throws Exception {
		return serviceProvider.getParticipantDataService().append(userId, participantDataId, data);
	}

	/**
	 * Append participant data rows for another participant
	 * 
	 * @param userId
	 * @param participantId
	 * @param participantDataId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = BridgeUrlHelpers.APPEND_FOR_PARTICIPANT_DATA, method = RequestMethod.POST)
	public @ResponseBody
	RowSet appendParticipantData(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@PathVariable String participantId, @PathVariable String participantDataId, @RequestBody RowSet data) throws Exception {
		return serviceProvider.getParticipantDataService().append(userId, participantId, participantDataId, data);
	}

	/**
	 * update existing participant data rows
	 * 
	 * @param userId
	 * @param participantDataId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_ID, method = RequestMethod.PUT)
	public @ResponseBody
	RowSet updateParticipantData(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@PathVariable String participantDataId, @RequestBody RowSet data) throws Exception {
		return serviceProvider.getParticipantDataService().update(userId, participantDataId, data);
	}

	/**
	 * get partitipant data
	 * 
	 * @param userId
	 * @param participantDataId
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedRowSet getParticipantData(
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@PathVariable String participantDataId) throws Exception {
		return serviceProvider.getParticipantDataService().get(userId, participantDataId, limit, offset);
	}

	/**
	 * Get the list of participant data for this user
	 * 
	 * @param limit
	 * @param offset
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ParticipantDataDescriptor> getUserParticipantDatas(
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getParticipantDataService().getUserParticipantDataDescriptors(userId, limit, offset);
	}

	/**
	 * create a new participant data description
	 * 
	 * @param userId
	 * @param participantDataDescriptor
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_DESCRIPTOR, method = RequestMethod.POST)
	public @ResponseBody
	ParticipantDataDescriptor createParticipantDataDescriptor(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@RequestBody ParticipantDataDescriptor participantDataDescriptor) throws Exception {
		return serviceProvider.getParticipantDataService().createParticipantDataDescriptor(userId, participantDataDescriptor);
	}

	/**
	 * get a participant data description
	 * 
	 * @param userId
	 * @param participantDataId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_DESCRIPTOR_ID, method = RequestMethod.GET)
	public @ResponseBody
	ParticipantDataDescriptor getParticipantDataDescriptor(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@PathVariable String participantDataId) throws DatastoreException, NotFoundException {
		return serviceProvider.getParticipantDataService().getParticipantDataDescriptor(userId, participantDataId);
	}

	/**
	 * create all participant data descriptions
	 * 
	 * @param limit
	 * @param offset
	 * @param userId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_DESCRIPTOR, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ParticipantDataDescriptor> getAllParticipantDatas(
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getParticipantDataService().getAllParticipantDataDescriptors(userId, limit, offset);
	}

	/**
	 * create a new participant data column description
	 * 
	 * @param userId
	 * @param participantDataColumnDescriptor
	 * @return
	 * @throws Exception
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_COLUMN_DESCRIPTORS, method = RequestMethod.POST)
	public @ResponseBody
	ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId,
			@RequestBody ParticipantDataColumnDescriptor participantDataColumnDescriptor) throws Exception {
		return serviceProvider.getParticipantDataService().createParticipantDataColumnDescriptor(userId, participantDataColumnDescriptor);
	}

	/**
	 * get all participant data column descriptions for a participant data descriptor
	 * 
	 * @param limit
	 * @param offset
	 * @param userId
	 * @param participantDataId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = BridgeUrlHelpers.PARTICIPANT_DATA_COLUMN_DESCRIPTORS_FOR_PARTICIPANT_DATA_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptors(
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) Long userId, @PathVariable String participantDataId)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getParticipantDataService().getParticipantDataColumnDescriptors(userId, participantDataId, limit, offset);
	}
}
