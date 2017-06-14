// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.documentapi;

import com.yahoo.document.Document;
import com.yahoo.document.DocumentId;
import com.yahoo.document.DocumentUpdate;
import com.yahoo.documentapi.messagebus.protocol.DocumentProtocol;

/**
 * <p>A session for asynchronous access to a document repository.
 * This class provides document repository writes and random access with high
 * throughput.</p>
 *
 * <p>All operations which are <i>accepted</i> by an async session will cause one or more
 * {@link Response responses} to be returned within the timeout limit. When an operation fails,
 * the response will contain the argument which was submitted to the operation.</p>
 *
 * @author bratseth
 */
public interface AsyncSession extends Session {

    /**
     * <p>Puts a document. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link DocumentResponse} objects to appear within the timeout time of this session.
     * The response returned later will either be a success, or contain the document submitted here.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param document the Document to put
     * @return the synchronous result of this operation
     */
    Result put(Document document);

    /**
     * <p>Puts a document. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link DocumentResponse} objects to appear within the timeout time of this session.
     * The response returned later will either be a success, or contain the document submitted here.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param document the Document to put
     * @return the synchronous result of this operation
     */
    Result put(Document document, DocumentProtocol.Priority priority);

    /**
     * <p>Gets a document. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link DocumentResponse} objects to appear within the timeout time of this session.
     * The response returned later will contain the requested document if it is a success.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param id the id of the document to get
     * @return the synchronous result of this operation
     * @throws UnsupportedOperationException if this access implementation does not support retrieving
     */
    Result get(DocumentId id);

    /**
     * <p>Gets a document. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link DocumentResponse} objects to appear within the timeout time of this session.
     * The response returned later will contain the requested document if it is a success.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param id the id of the document to get
     * @param priority The priority with which to perform this operation.
     * @return the synchronous result of this operation
     * @throws UnsupportedOperationException if this access implementation does not support retrieving
     */
    Result get(DocumentId id, boolean headersOnly, DocumentProtocol.Priority priority);

    /**
     * <p>Removes a document if it is present. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link RemoveResponse} objects to appear within the timeout time of this session.
     * The response returned later will either be a success, or contain the document id submitted here.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param id the id of the document to remove
     * @return the synchronous result of this operation
     * @throws UnsupportedOperationException if this access implementation does not support removal
     */
    Result remove(DocumentId id);

    /**
     * <p>Removes a document if it is present. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link DocumentIdResponse} objects to apprear within the timeout time of this session.
     * The response returned later will either be a success, or contain the document id submitted here.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param id the id of the document to remove
     * @param priority The priority with which to perform this operation.
     * @return the synchronous result of this operation
     * @throws UnsupportedOperationException if this access implementation does not support removal
     */
    Result remove(DocumentId id, DocumentProtocol.Priority priority);

    /**
     * <p>Updates a document. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link DocumentUpdateResponse} within the timeout time of this session.
     * The returned response returned later will either be a success or contain the update submitted here.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param update the updates to perform
     * @return the synchronous result of this operation
     * @throws UnsupportedOperationException if this access implementation does not support update
     */
    Result update(DocumentUpdate update);

    /**
     * <p>Updates a document. This method returns immediately.</p>
     *
     * <p>If this result is a success, this
     * call will cause one or more {@link DocumentUpdateResponse} within the timeout time of this session.
     * The returned response returned later will either be a success or contain the update submitted here.
     * If it was not a success, this method has no further effects.</p>
     *
     * @param update the updates to perform
     * @param priority The priority with which to perform this operation.
     * @return the synchronous result of this operation
     * @throws UnsupportedOperationException if this access implementation does not support update
     */
    Result update(DocumentUpdate update, DocumentProtocol.Priority priority);

    /**
     * Returns the current send window size of the session.
     *
     * @return Returns the window size.
     */
    double getCurrentWindowSize();

}
