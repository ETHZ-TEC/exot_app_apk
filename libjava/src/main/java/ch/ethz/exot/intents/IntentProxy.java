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

public final class IntentProxy {
    protected static final String BASE = "ch.ethz.exot.intents.IntentProxy";

    public final class Action {
        private static final String BASE_A = BASE + ".action.";
        public static final String BUNDLE_EXTRAS  = BASE_A + "BUNDLE_EXTRAS";
        public static final String START_APPS     = BASE_A + "START_APPS";
        public static final String STOP_APPS      = BASE_A + "STOP_APPS";
        public static final String CONFIGURE_APP  = BASE_A + "CONFIGURE_APP";
        public static final String FORWARD_       = BASE_A + "FORWARD_";
        public static final String FORWARD        = BASE_A + "FORWARD";
        public static final String FORWARD_BUNDLE = BASE_A + "FORWARD_BUNDLE";
        public static final String FORWARD_STARTSERVICE = BASE_A + "FORWARD_STARTSERVICE";
        public static final String FORWARD_STARTACTIVITY = BASE_A + "FORWARD_STARTACTIVITY";
    }

    public final class KeysExtras {
        // Special extra keys which require parsing and are not directly put into the extra bundle
        private static final String BASE_KE  = BASE + ".keyextra.";
        public static final String INTENT_ACTION     = "intent.action";
        public static final String INTENT_COMPONENT  = "intent.component";
        public static final String INTENT_FLAGS      = "intent.flags";
        public static final String INTENT_EXTRAS_KEY = "intent.extra.key";

        // Keys for repackaging
        public static final String DEFAULT_BUNDLE    = BASE_KE + "BUNDLE";

        // Other Keys
        public static final String APPS_ARRAY        = BASE_KE + "APPS_ARRAY";
    }
}
