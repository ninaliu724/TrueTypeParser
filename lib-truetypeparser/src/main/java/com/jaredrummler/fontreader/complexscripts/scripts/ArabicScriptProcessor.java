/*
 * Copyright (C) 2016 Jared Rummler <jared.rummler@gmail.com>
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
 *
 */

package com.jaredrummler.fontreader.complexscripts.scripts;

import com.jaredrummler.fontreader.complexscripts.bidi.BidiClass;
import com.jaredrummler.fontreader.complexscripts.bidi.BidiConstants;
import com.jaredrummler.fontreader.complexscripts.util.CharAssociation;
import com.jaredrummler.fontreader.util.CharUtilities;
import com.jaredrummler.fontreader.util.GlyphSequence;
import com.jaredrummler.fontreader.util.ScriptContextTester;
import com.jaredrummler.fontreader.complexscripts.fonts.GlyphContextTester;
import com.jaredrummler.fontreader.complexscripts.fonts.GlyphDefinitionTable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The <code>ArabicScriptProcessor</code> class implements a script processor for
 * performing glyph substitution and positioning operations on content associated with the Arabic script.</p>
 *
 * <p>This work was originally authored by Glenn Adams (gadams@apache.org).</p>
 */
public class ArabicScriptProcessor extends DefaultScriptProcessor {

  /** features to use for substitutions */
  private static final String[] GSUB_FEATURES = {
      "calt",                                                 // contextual alternates
      "ccmp",                                                 // glyph composition/decomposition
      "fina",                                                 // final (terminal) forms
      "init",                                                 // initial forms
      "isol",                                                 // isolated formas
      "liga",                                                 // standard ligatures
      "medi",                                                 // medial forms
      "rlig"                                                  // required ligatures
  };

  /** features to use for positioning */
  private static final String[] GPOS_FEATURES = {
      "curs",                                                 // cursive positioning
      "kern",                                                 // kerning
      "mark",                                                 // mark to base or ligature positioning
      "mkmk"                                                  // mark to mark positioning
  };

  private static class SubstitutionScriptContextTester implements ScriptContextTester {

    private static Map/*<String,GlyphContextTester>*/ testerMap = new HashMap/*<String,GlyphContextTester>*/();

    static {
      testerMap.put("fina", new GlyphContextTester() {

        public boolean test(String script, String language, String feature, GlyphSequence gs, int index, int flags) {
          return inFinalContext(script, language, feature, gs, index, flags);
        }
      });
      testerMap.put("init", new GlyphContextTester() {

        public boolean test(String script, String language, String feature, GlyphSequence gs, int index, int flags) {
          return inInitialContext(script, language, feature, gs, index, flags);
        }
      });
      testerMap.put("isol", new GlyphContextTester() {

        public boolean test(String script, String language, String feature, GlyphSequence gs, int index, int flags) {
          return inIsolateContext(script, language, feature, gs, index, flags);
        }
      });
      testerMap.put("liga", new GlyphContextTester() {

        public boolean test(String script, String language, String feature, GlyphSequence gs, int index, int flags) {
          return inLigatureContext(script, language, feature, gs, index, flags);
        }
      });
      testerMap.put("medi", new GlyphContextTester() {

        public boolean test(String script, String language, String feature, GlyphSequence gs, int index, int flags) {
          return inMedialContext(script, language, feature, gs, index, flags);
        }
      });
    }

    public GlyphContextTester getTester(String feature) {
      return (GlyphContextTester) testerMap.get(feature);
    }
  }

  private static class PositioningScriptContextTester implements ScriptContextTester {

    private static Map/*<String,GlyphContextTester>*/ testerMap = new HashMap/*<String,GlyphContextTester>*/();

    public GlyphContextTester getTester(String feature) {
      return (GlyphContextTester) testerMap.get(feature);
    }
  }

  private final ScriptContextTester subContextTester;
  private final ScriptContextTester posContextTester;

  ArabicScriptProcessor(String script) {
    super(script);
    this.subContextTester = new SubstitutionScriptContextTester();
    this.posContextTester = new PositioningScriptContextTester();
  }

  /** {@inheritDoc} */
  public String[] getSubstitutionFeatures() {
    return GSUB_FEATURES;
  }

  /** {@inheritDoc} */
  public ScriptContextTester getSubstitutionContextTester() {
    return subContextTester;
  }

  /** {@inheritDoc} */
  public String[] getPositioningFeatures() {
    return GPOS_FEATURES;
  }

  /** {@inheritDoc} */
  public ScriptContextTester getPositioningContextTester() {
    return posContextTester;
  }

  /** {@inheritDoc} */
  @Override
  public GlyphSequence reorderCombiningMarks(GlyphDefinitionTable gdef, GlyphSequence gs, int[] widths, int[][] gpa,
                                             String script, String language) {
    // a side effect of BIDI reordering is to order combining marks before their base, so we need to override the default here to
    // prevent double reordering
    return gs;
  }

  private static boolean inFinalContext(String script, String language, String feature, GlyphSequence gs, int index,
                                        int flags) {
    CharAssociation a = gs.getAssociation(index);
    int[] ca = gs.getCharacterArray(false);
    int nc = gs.getCharacterCount();
    if (nc == 0) {
      return false;
    } else {
      int s = a.getStart();
      int e = a.getEnd();
      if (!hasFinalPrecedingContext(ca, nc, s, e)) {
        return false;
      } else if (!hasFinalThisContext(ca, nc, s, e)) {
        return false;
      } else if (forceFinalThisContext(ca, nc, s, e)) {
        return true;
      } else return hasFinalSucceedingContext(ca, nc, s, e);
    }
  }

  private static boolean inInitialContext(String script, String language, String feature, GlyphSequence gs, int index,
                                          int flags) {
    CharAssociation a = gs.getAssociation(index);
    int[] ca = gs.getCharacterArray(false);
    int nc = gs.getCharacterCount();
    if (nc == 0) {
      return false;
    } else {
      int s = a.getStart();
      int e = a.getEnd();
      if (!hasInitialPrecedingContext(ca, nc, s, e)) {
        return false;
      } else if (!hasInitialThisContext(ca, nc, s, e)) {
        return false;
      } else return hasInitialSucceedingContext(ca, nc, s, e);
    }
  }

  private static boolean inIsolateContext(String script, String language, String feature, GlyphSequence gs, int index,
                                          int flags) {
    CharAssociation a = gs.getAssociation(index);
    int nc = gs.getCharacterCount();
    if (nc == 0) {
      return false;
    } else return (a.getStart() == 0) && (a.getEnd() == nc);
  }

  private static boolean inLigatureContext(String script, String language, String feature, GlyphSequence gs, int index,
                                           int flags) {
    CharAssociation a = gs.getAssociation(index);
    int[] ca = gs.getCharacterArray(false);
    int nc = gs.getCharacterCount();
    if (nc == 0) {
      return false;
    } else {
      int s = a.getStart();
      int e = a.getEnd();
      if (!hasLigaturePrecedingContext(ca, nc, s, e)) {
        return false;
      } else return hasLigatureSucceedingContext(ca, nc, s, e);
    }
  }

  private static boolean inMedialContext(String script, String language, String feature, GlyphSequence gs, int index,
                                         int flags) {
    CharAssociation a = gs.getAssociation(index);
    int[] ca = gs.getCharacterArray(false);
    int nc = gs.getCharacterCount();
    if (nc == 0) {
      return false;
    } else {
      int s = a.getStart();
      int e = a.getEnd();
      if (!hasMedialPrecedingContext(ca, nc, s, e)) {
        return false;
      } else if (!hasMedialThisContext(ca, nc, s, e)) {
        return false;
      } else return hasMedialSucceedingContext(ca, nc, s, e);
    }
  }

  private static boolean hasFinalPrecedingContext(int[] ca, int nc, int s, int e) {
    int chp = 0;    // preceding non-NSM char in [0,s) searching back from s
    int clp = 0;
    for (int i = s; i > 0; i--) {
      int k = i - 1;
      if ((k >= 0) && (k < nc)) {
        chp = ca[k];
        clp = BidiClass.getBidiClass(chp);
        if (clp != BidiConstants.NSM) {
          break;
        }
      }
    }
    if (clp != BidiConstants.AL) {
      return isZWJ(chp);
    } else return !hasIsolateInitial(chp);
  }

  private static boolean hasFinalThisContext(int[] ca, int nc, int s, int e) {
    int chl = 0;    // last non-{NSM,ZWJ} char in [s,e)
    int cll = 0;
    for (int i = 0, n = e - s; i < n; i++) {
      int k = n - i - 1;
      int j = s + k;
      if ((j >= 0) && (j < nc)) {
        chl = ca[j];
        cll = BidiClass.getBidiClass(chl);
        if ((cll != BidiConstants.NSM) && !isZWJ(chl)) {
          break;
        }
      }
    }
    if (cll != BidiConstants.AL) {
      return false;
    }
    return !hasIsolateFinal(chl);
  }

  private static boolean forceFinalThisContext(int[] ca, int nc, int s, int e) {
    int chl = 0;    // last non-{NSM,ZWJ} char in [s,e)
    int cll = 0;
    for (int i = 0, n = e - s; i < n; i++) {
      int k = n - i - 1;
      int j = s + k;
      if ((j >= 0) && (j < nc)) {
        chl = ca[j];
        cll = BidiClass.getBidiClass(chl);
        if ((cll != BidiConstants.NSM) && !isZWJ(chl)) {
          break;
        }
      }
    }
    if (cll != BidiConstants.AL) {
      return false;
    }
    return hasIsolateInitial(chl);
  }

  private static boolean hasFinalSucceedingContext(int[] ca, int nc, int s, int e) {
    int chs = 0;    // succeeding non-NSM char in [e,nc) searching forward from e
    int cls = 0;
    for (int i = e, n = nc; i < n; i++) {
      chs = ca[i];
      cls = BidiClass.getBidiClass(chs);
      if (cls != BidiConstants.NSM) {
        break;
      }
    }
    if (cls != BidiConstants.AL) {
      return !isZWJ(chs);
    } else return hasIsolateFinal(chs);
  }

  private static boolean hasInitialPrecedingContext(int[] ca, int nc, int s, int e) {
    int chp = 0;    // preceding non-NSM char in [0,s) searching back from s
    int clp = 0;
    for (int i = s; i > 0; i--) {
      int k = i - 1;
      if ((k >= 0) && (k < nc)) {
        chp = ca[k];
        clp = BidiClass.getBidiClass(chp);
        if (clp != BidiConstants.NSM) {
          break;
        }
      }
    }
    if (clp != BidiConstants.AL) {
      return !isZWJ(chp);
    } else return hasIsolateInitial(chp);
  }

  private static boolean hasInitialThisContext(int[] ca, int nc, int s, int e) {
    int chf = 0;    // first non-{NSM,ZWJ} char in [s,e)
    int clf = 0;
    for (int i = 0, n = e - s; i < n; i++) {
      int k = s + i;
      if ((k >= 0) && (k < nc)) {
        chf = ca[s + i];
        clf = BidiClass.getBidiClass(chf);
        if ((clf != BidiConstants.NSM) && !isZWJ(chf)) {
          break;
        }
      }
    }
    if (clf != BidiConstants.AL) {
      return false;
    }
    return !hasIsolateInitial(chf);
  }

  private static boolean hasInitialSucceedingContext(int[] ca, int nc, int s, int e) {
    int chs = 0;    // succeeding non-NSM char in [e,nc) searching forward from e
    int cls = 0;
    for (int i = e, n = nc; i < n; i++) {
      chs = ca[i];
      cls = BidiClass.getBidiClass(chs);
      if (cls != BidiConstants.NSM) {
        break;
      }
    }
    if (cls != BidiConstants.AL) {
      return isZWJ(chs);
    } else return !hasIsolateFinal(chs);
  }

  private static boolean hasMedialPrecedingContext(int[] ca, int nc, int s, int e) {
    int chp = 0;    // preceding non-NSM char in [0,s) searching back from s
    int clp = 0;
    for (int i = s; i > 0; i--) {
      int k = i - 1;
      if ((k >= 0) && (k < nc)) {
        chp = ca[k];
        clp = BidiClass.getBidiClass(chp);
        if (clp != BidiConstants.NSM) {
          break;
        }
      }
    }
    if (clp != BidiConstants.AL) {
      return isZWJ(chp);
    } else return !hasIsolateInitial(chp);
  }

  private static boolean hasMedialThisContext(int[] ca, int nc, int s, int e) {
    int chf = 0;    // first non-{NSM,ZWJ} char in [s,e)
    int clf = 0;
    for (int i = 0, n = e - s; i < n; i++) {
      int k = s + i;
      if ((k >= 0) && (k < nc)) {
        chf = ca[s + i];
        clf = BidiClass.getBidiClass(chf);
        if ((clf != BidiConstants.NSM) && !isZWJ(chf)) {
          break;
        }
      }
    }
    if (clf != BidiConstants.AL) {
      return false;
    }
    int chl = 0;    // last non-{NSM,ZWJ} char in [s,e)
    int cll = 0;
    for (int i = 0, n = e - s; i < n; i++) {
      int k = n - i - 1;
      int j = s + k;
      if ((j >= 0) && (j < nc)) {
        chl = ca[j];
        cll = BidiClass.getBidiClass(chl);
        if ((cll != BidiConstants.NSM) && !isZWJ(chl)) {
          break;
        }
      }
    }
    if (cll != BidiConstants.AL) {
      return false;
    }
    if (hasIsolateFinal(chf)) {
      return false;
    } else return !hasIsolateInitial(chl);
  }

  private static boolean hasMedialSucceedingContext(int[] ca, int nc, int s, int e) {
    int chs = 0;    // succeeding non-NSM char in [e,nc) searching forward from e
    int cls = 0;
    for (int i = e, n = nc; i < n; i++) {
      chs = ca[i];
      cls = BidiClass.getBidiClass(chs);
      if (cls != BidiConstants.NSM) {
        break;
      }
    }
    if (cls != BidiConstants.AL) {
      return isZWJ(chs);
    } else return !hasIsolateFinal(chs);
  }

  private static boolean hasLigaturePrecedingContext(int[] ca, int nc, int s, int e) {
    return true;
  }

  private static boolean hasLigatureSucceedingContext(int[] ca, int nc, int s, int e) {
    int chs = 0;    // succeeding non-NSM char in [e,nc) searching forward from e
    int cls = 0;
    for (int i = e, n = nc; i < n; i++) {
      chs = ca[i];
      cls = BidiClass.getBidiClass(chs);
      // TBD - does ZWJ have impact here?
      if (cls != BidiConstants.NSM) {
        break;
      }
    }
    return cls == BidiConstants.AL;
  }

  /**
   * Ordered array of Unicode scalars designating those Arabic (Script) Letters
   * which exhibit an isolated form in word initial position.
   */
  private static final int[] ISOLATED_INITIALS = {
      0x0621, // HAMZA
      0x0622, // ALEF WITH MADDA ABOVE
      0x0623, // ALEF WITH HAMZA ABOVE
      0x0624, // WAW WITH HAMZA ABOVE
      0x0625, // ALEF WITH HAMZA BELOWW
      0x0627, // ALEF
      0x062F, // DAL
      0x0630, // THAL
      0x0631, // REH
      0x0632, // ZAIN
      0x0648, // WAW
      0x0671, // ALEF WASLA
      0x0672, // ALEF WITH WAVY HAMZA ABOVE
      0x0673, // ALEF WITH WAVY HAMZA BELOW
      0x0675, // HIGH HAMZA ALEF
      0x0676, // HIGH HAMZA WAW
      0x0677, // U WITH HAMZA ABOVE
      0x0688, // DDAL
      0x0689, // DAL WITH RING
      0x068A, // DAL WITH DOT BELOW
      0x068B, // DAL WITH DOT BELOW AND SMALL TAH
      0x068C, // DAHAL
      0x068D, // DDAHAL
      0x068E, // DUL
      0x068F, // DUL WITH THREE DOTS ABOVE DOWNWARDS
      0x0690, // DUL WITH FOUR DOTS ABOVE
      0x0691, // RREH
      0x0692, // REH WITH SMALL V
      0x0693, // REH WITH RING
      0x0694, // REH WITH DOT BELOW
      0x0695, // REH WITH SMALL V BELOW
      0x0696, // REH WITH DOT BELOW AND DOT ABOVE
      0x0697, // REH WITH TWO DOTS ABOVE
      0x0698, // JEH
      0x0699, // REH WITH FOUR DOTS ABOVE
      0x06C4, // WAW WITH RING
      0x06C5, // KIRGHIZ OE
      0x06C6, // OE
      0x06C7, // U
      0x06C8, // YU
      0x06C9, // KIRGHIZ YU
      0x06CA, // WAW WITH TWO DOTS ABOVE
      0x06CB, // VE
      0x06CF, // WAW WITH DOT ABOVE
      0x06EE, // DAL WITH INVERTED V
      0x06EF  // REH WITH INVERTED V
  };

  private static boolean hasIsolateInitial(int ch) {
    return Arrays.binarySearch(ISOLATED_INITIALS, ch) >= 0;
  }

  /**
   * Ordered array of Unicode scalars designating those Arabic (Script) Letters
   * which exhibit an isolated form in word final position.
   */
  private static final int[] ISOLATED_FINALS = {
      0x0621  // HAMZA
  };

  private static boolean hasIsolateFinal(int ch) {
    return Arrays.binarySearch(ISOLATED_FINALS, ch) >= 0;
  }

  private static boolean isZWJ(int ch) {
    return ch == CharUtilities.ZERO_WIDTH_JOINER;
  }

}
