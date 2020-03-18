/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package zalopay.event;

/**
 * @author phuctt4
 */
public enum OperationEvent {
    /**
     * ADD_GLOBAL_SESSION event type.
     */
    ADD_GLOBAL_SESSION,
    /**
     * UPDATE_GLOBAL_SESSION event type.
     */
    UPDATE_GLOBAL_SESSION,
    /**
     * DELETE_GLOBAL_SESSION event type.
     */
    DELETE_GLOBAL_SESSION,
    /**
     * ADD_BRANCH_SESSION event type.
     */
    ADD_BRANCH_SESSION,
    /**
     * UPDATE_BRANCH_SESSION event type.
     */
    UPDATE_BRANCH_SESSION,
    /**
     * DELETE_BRANCH_SESSION event type.
     */
    DELETE_BRANCH_SESSION
}
