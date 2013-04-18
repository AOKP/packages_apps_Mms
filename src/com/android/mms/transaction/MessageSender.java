/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.transaction;

import java.util.Map;
import java.util.HashMap;

import android.os.CountDownTimer;

import com.google.android.mms.MmsException;

public interface MessageSender {
    public static final String RECIPIENTS_SEPARATOR = ";";

    /**
     * Send the message through MMS or SMS protocol.
     * @param token The token to identify the sending progress.
     *
     * @return True if the message was sent through MMS or false if it was
     *         sent through SMS.
     * @throws MmsException Error occurred while sending the message.
     */
    boolean sendMessage(long token) throws MmsException;

    /**
     * Countdown class which can be implemented to delay sending the message.
     */
    public abstract class AbstractCountDownSender extends CountDownTimer {
        private final static long countDownInterval = 500;
        long remainingTime;

        public AbstractCountDownSender(long millisInFuture) {
            // We add 500ms because the user doesn't want to see the counter quickly starting
            super(millisInFuture + 500, countDownInterval);
        }

        public void onTick(long millisUntilFinished) {
            remainingTime = millisUntilFinished;
        }

        public int getRemainingSeconds() {
            return (int) remainingTime / 1000;
        }
    }

    /**
     * Map containing the spawned countdowns used to delay sending messages
     */
    public static final Map<Long, AbstractCountDownSender> mCountDownSenders = new HashMap<Long, AbstractCountDownSender>();
}

