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

public final class ExOTApps {
    protected static final String BASE       = "ch.ethz.exot.intents.exotapps";

    public final class Broadcasts {
        private static final String BASE_B = BASE + ".broadcast.";

        public static final String STATUS = BASE_B + "STATUS";
        public static final String KILLED = BASE_B + "KILLED";
        public static final String EXCEPTION = BASE_B + "EXCEPTION";
    }

    public final class Actions {
        private static final String BASE_A     = BASE + ".action.";
        /**
         * Possible actions performed by the service.
         */
        public static final String START       = BASE_A + "START";
        public static final String STOP        = BASE_A + "STOP";
        public static final String CREATE      = BASE_A + "CREATE";
        public static final String RESET       = BASE_A + "RESET";
        public static final String DESTROY     = BASE_A + "DESTROY";
        public static final String QUERY       = BASE_A + "QUERY";
        public static final String STATUS      = BASE_A + "STATUS";
        public static final String KILLED      = BASE_A + "KILLED";
    }

    public final class Keys {
        private static final String BASE_KE    = BASE + ".keyextra.";
        public static final String STATUS      = BASE_KE + "STATUS";
        public static final String DATA_PATH   = BASE_KE + "DATA_PATH";
        public static final String UUID        = BASE_KE + "UUID";
        public static final String REOPENED    = BASE_KE + "REOPENED";
        public static final String MODE        = BASE_KE + "MODE";
        public static final String BUNDLE      = BASE_KE + "BUNDLE";;
        public static final String CONFIG      = BASE_KE + "CONFIG";
    }

    /**
     * Enumeration for choosing between standard and advanced modes.
     */
    public enum Modes {
        // TODO @klopottb: What are these modes?!
        ADVANCED,
        NORMAL
    }

    /**
     * Enumeration with the status of the ExOTApps-Service.
     */
    public enum Status {
        MISSING,
        CREATED,
        INITIALISED,
        RUNNING
    }
}
