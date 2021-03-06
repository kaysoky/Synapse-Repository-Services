package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.PaginatedRowSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataManager {

	RowSet appendData(UserInfo userInfo, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException;

	RowSet appendData(UserInfo userInfo, String participantId, String participantDataId, RowSet data) throws DatastoreException,
			NotFoundException, IOException;

	RowSet updateData(UserInfo userInfo, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException;

	PaginatedRowSet getData(UserInfo userInfo, String participantDataId, Integer limit, Integer offset) throws DatastoreException,
			NotFoundException, IOException;
}
