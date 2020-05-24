// Copyright (c) 2015-2020, Swiss Federal Institute of Technology (ETH Zurich)
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// * Redistributions of source code must retain the above copyright notice, this
//   list of conditions and the following disclaimer.
// 
// * Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
// 
// * Neither the name of the copyright holder nor the names of its
//   contributors may be used to endorse or promote products derived from
//   this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 
package ch.ethz.exot.intents;

public final class RepetiTouch {
    public final class Action {
        public static final String FIRE_SETTING    = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
        public static final String REQUEST_QUERY   = "com.twofortyfouram.locale.intent.action.REQUEST_QUERY";
        public static final String QUERY_CONDITION = "com.twofortyfouram.locale.intent.action.QUERY_CONDITION";
    }

    public final class KeysExtras {
        public static final String ACTIVITY = "com.twofortyfouram.locale.intent.extra.ACTIVITY";
        public static final String BUNDLE   = "com.twofortyfouram.locale.intent.extra.BUNDLE";
        public static final String BLURB    = "com.twofortyfouram.locale.intent.extra.BLURB";
    }

    public final class KeysBundleExtras {
        public static final String ACTION             = "action";
        public static final String CONDITION          = "condition";
        public static final String FILENAME           = "filename";
        public static final String APPENDING_RECORD   = "appendingrecord";
        public static final String LOOP_TIMES         = "looptimes";
        public static final String REPLAY_SPEED       = "replayspeed";
        public static final String HIDE_PANEL         = "hidepanel";
        public static final String CLOSE_AFTER_ACTION = "closeafteraction";
        public static final String SILENT             = "silent";
    }

    public final class ConditionQueryCodes {
        public static final int SATISFIED   = 16;
        public static final int UNSATISFIED = 17;
        public static final int UNKNOWN     = 18;
    }

    public final class ValueBundleExtraAction {
        public static final String START_RECORD = "Start Record";
        public static final String STOP_RECORD  = "Stop Record";
        public static final String START_REPLAY = "Start Replay";
        public static final String STOP_REPLAY  = "Stop Replay";
        public static final String LOAD_FILE    = "Load File";
        public static final String SAVE_FILE    = "Save File";
    }
}
