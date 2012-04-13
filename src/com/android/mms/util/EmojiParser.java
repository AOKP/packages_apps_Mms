/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mms.util;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import com.android.mms.R;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for annotating a CharSequence with spans to convert textual Unicode
 * Softbank emojis to graphical ones.
 */
public class EmojiParser {
    // Singleton stuff
    private static EmojiParser sInstance;

    public static EmojiParser getInstance() {
        return sInstance;
    }

    public static void init(Context context) {
        sInstance = new EmojiParser(context);
    }

    private final Context mContext;
    private final String[] mSmileyTexts;
    private final Pattern mPattern;
    private final HashMap<String, Integer> mSmileyToRes;

    private EmojiParser(Context context) {
        mContext = context;
        mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_EMOJI_TEXTS);
        mSmileyToRes = buildSmileyToRes();
        mPattern = buildPattern();
    }

    static class Emojis {
        private static final int[] sIconIds = { R.drawable.emoji_e415, R.drawable.emoji_e056, R.drawable.emoji_e057,
                R.drawable.emoji_e414, R.drawable.emoji_e405, R.drawable.emoji_e106, R.drawable.emoji_e418,
                R.drawable.emoji_e417, R.drawable.emoji_e40d, R.drawable.emoji_e40a, R.drawable.emoji_e404,
                R.drawable.emoji_e105, R.drawable.emoji_e409, R.drawable.emoji_e40e, R.drawable.emoji_e402,
                R.drawable.emoji_e108, R.drawable.emoji_e403, R.drawable.emoji_e058, R.drawable.emoji_e407,
                R.drawable.emoji_e401, R.drawable.emoji_e40f, R.drawable.emoji_e40b, R.drawable.emoji_e406,
                R.drawable.emoji_e413, R.drawable.emoji_e411, R.drawable.emoji_e412, R.drawable.emoji_e410,
                R.drawable.emoji_e107, R.drawable.emoji_e059, R.drawable.emoji_e416, R.drawable.emoji_e408,
                R.drawable.emoji_e40c, R.drawable.emoji_e11a, R.drawable.emoji_e10c, R.drawable.emoji_e32c,
                R.drawable.emoji_e32a, R.drawable.emoji_e32d, R.drawable.emoji_e328, R.drawable.emoji_e32b,
                R.drawable.emoji_e022, R.drawable.emoji_e023, R.drawable.emoji_e327, R.drawable.emoji_e329,
                R.drawable.emoji_e32e, R.drawable.emoji_e32f, R.drawable.emoji_e335, R.drawable.emoji_e334,
                R.drawable.emoji_e021, R.drawable.emoji_e337, R.drawable.emoji_e020, R.drawable.emoji_e336,
                R.drawable.emoji_e13c, R.drawable.emoji_e330, R.drawable.emoji_e331, R.drawable.emoji_e326,
                R.drawable.emoji_e03e, R.drawable.emoji_e11d, R.drawable.emoji_e05a, R.drawable.emoji_e00e,
                R.drawable.emoji_e421, R.drawable.emoji_e420, R.drawable.emoji_e00d, R.drawable.emoji_e010,
                R.drawable.emoji_e011, R.drawable.emoji_e41e, R.drawable.emoji_e012, R.drawable.emoji_e422,
                R.drawable.emoji_e22e, R.drawable.emoji_e22f, R.drawable.emoji_e231, R.drawable.emoji_e230,
                R.drawable.emoji_e427, R.drawable.emoji_e41d, R.drawable.emoji_e00f, R.drawable.emoji_e41f,
                R.drawable.emoji_e14c, R.drawable.emoji_e201, R.drawable.emoji_e115, R.drawable.emoji_e428,
                R.drawable.emoji_e51f, R.drawable.emoji_e429, R.drawable.emoji_e424, R.drawable.emoji_e423,
                R.drawable.emoji_e253, R.drawable.emoji_e426, R.drawable.emoji_e111, R.drawable.emoji_e425,
                R.drawable.emoji_e31e, R.drawable.emoji_e31f, R.drawable.emoji_e31d, R.drawable.emoji_e001,
                R.drawable.emoji_e002, R.drawable.emoji_e005, R.drawable.emoji_e004, R.drawable.emoji_e51a,
                R.drawable.emoji_e519, R.drawable.emoji_e518, R.drawable.emoji_e515, R.drawable.emoji_e516,
                R.drawable.emoji_e517, R.drawable.emoji_e51b, R.drawable.emoji_e152, R.drawable.emoji_e04e,
                R.drawable.emoji_e51c, R.drawable.emoji_e51e, R.drawable.emoji_e11c, R.drawable.emoji_e536,
                R.drawable.emoji_e003, R.drawable.emoji_e41c, R.drawable.emoji_e41b, R.drawable.emoji_e419,
                R.drawable.emoji_e41a, R.drawable.emoji_e04a, R.drawable.emoji_e04b, R.drawable.emoji_e049,
                R.drawable.emoji_e048, R.drawable.emoji_e04c, R.drawable.emoji_e13d, R.drawable.emoji_e443,
                R.drawable.emoji_e43e, R.drawable.emoji_e04f, R.drawable.emoji_e052, R.drawable.emoji_e053,
                R.drawable.emoji_e524, R.drawable.emoji_e52c, R.drawable.emoji_e52a, R.drawable.emoji_e531,
                R.drawable.emoji_e050, R.drawable.emoji_e527, R.drawable.emoji_e051, R.drawable.emoji_e10b,
                R.drawable.emoji_e52b, R.drawable.emoji_e52f, R.drawable.emoji_e528, R.drawable.emoji_e01a,
                R.drawable.emoji_e134, R.drawable.emoji_e530, R.drawable.emoji_e529, R.drawable.emoji_e526,
                R.drawable.emoji_e52d, R.drawable.emoji_e521, R.drawable.emoji_e523, R.drawable.emoji_e52e,
                R.drawable.emoji_e055, R.drawable.emoji_e525, R.drawable.emoji_e10a, R.drawable.emoji_e109,
                R.drawable.emoji_e522, R.drawable.emoji_e019, R.drawable.emoji_e054, R.drawable.emoji_e520,
                R.drawable.emoji_e306, R.drawable.emoji_e030, R.drawable.emoji_e304, R.drawable.emoji_e110,
                R.drawable.emoji_e032, R.drawable.emoji_e305, R.drawable.emoji_e303, R.drawable.emoji_e118,
                R.drawable.emoji_e447, R.drawable.emoji_e119, R.drawable.emoji_e307, R.drawable.emoji_e308,
                R.drawable.emoji_e444, R.drawable.emoji_e441, R.drawable.emoji_e436, R.drawable.emoji_e437,
                R.drawable.emoji_e438, R.drawable.emoji_e43a, R.drawable.emoji_e439, R.drawable.emoji_e43b,
                R.drawable.emoji_e117, R.drawable.emoji_e440, R.drawable.emoji_e442, R.drawable.emoji_e446,
                R.drawable.emoji_e445, R.drawable.emoji_e11b, R.drawable.emoji_e448, R.drawable.emoji_e033,
                R.drawable.emoji_e112, R.drawable.emoji_e325, R.drawable.emoji_e312, R.drawable.emoji_e310,
                R.drawable.emoji_e126, R.drawable.emoji_e127, R.drawable.emoji_e008, R.drawable.emoji_e03d,
                R.drawable.emoji_e00c, R.drawable.emoji_e12a, R.drawable.emoji_e00a, R.drawable.emoji_e00b,
                R.drawable.emoji_e009, R.drawable.emoji_e316, R.drawable.emoji_e129, R.drawable.emoji_e141,
                R.drawable.emoji_e142, R.drawable.emoji_e317, R.drawable.emoji_e128, R.drawable.emoji_e14b,
                R.drawable.emoji_e211, R.drawable.emoji_e114, R.drawable.emoji_e145, R.drawable.emoji_e144,
                R.drawable.emoji_e03f, R.drawable.emoji_e313, R.drawable.emoji_e116, R.drawable.emoji_e10f,
                R.drawable.emoji_e104, R.drawable.emoji_e103, R.drawable.emoji_e101, R.drawable.emoji_e102,
                R.drawable.emoji_e13f, R.drawable.emoji_e140, R.drawable.emoji_e11f, R.drawable.emoji_e12f,
                R.drawable.emoji_e031, R.drawable.emoji_e30e, R.drawable.emoji_e311, R.drawable.emoji_e113,
                R.drawable.emoji_e30f, R.drawable.emoji_e13b, R.drawable.emoji_e42b, R.drawable.emoji_e42a,
                R.drawable.emoji_e018, R.drawable.emoji_e016, R.drawable.emoji_e015, R.drawable.emoji_e014,
                R.drawable.emoji_e42c, R.drawable.emoji_e42d, R.drawable.emoji_e017, R.drawable.emoji_e013,
                R.drawable.emoji_e20e, R.drawable.emoji_e20c, R.drawable.emoji_e20f, R.drawable.emoji_e20d,
                R.drawable.emoji_e131, R.drawable.emoji_e12b, R.drawable.emoji_e130, R.drawable.emoji_e12d,
                R.drawable.emoji_e324, R.drawable.emoji_e301, R.drawable.emoji_e148, R.drawable.emoji_e502,
                R.drawable.emoji_e03c, R.drawable.emoji_e30a, R.drawable.emoji_e042, R.drawable.emoji_e040,
                R.drawable.emoji_e041, R.drawable.emoji_e12c, R.drawable.emoji_e007, R.drawable.emoji_e31a,
                R.drawable.emoji_e13e, R.drawable.emoji_e31b, R.drawable.emoji_e006, R.drawable.emoji_e302,
                R.drawable.emoji_e319, R.drawable.emoji_e321, R.drawable.emoji_e322, R.drawable.emoji_e314,
                R.drawable.emoji_e503, R.drawable.emoji_e10e, R.drawable.emoji_e318, R.drawable.emoji_e43c,
                R.drawable.emoji_e11e, R.drawable.emoji_e323, R.drawable.emoji_e31c, R.drawable.emoji_e034,
                R.drawable.emoji_e035, R.drawable.emoji_e045, R.drawable.emoji_e338, R.drawable.emoji_e047,
                R.drawable.emoji_e30c, R.drawable.emoji_e044, R.drawable.emoji_e30b, R.drawable.emoji_e043,
                R.drawable.emoji_e120, R.drawable.emoji_e33b, R.drawable.emoji_e33f, R.drawable.emoji_e341,
                R.drawable.emoji_e34c, R.drawable.emoji_e344, R.drawable.emoji_e342, R.drawable.emoji_e33d,
                R.drawable.emoji_e33e, R.drawable.emoji_e340, R.drawable.emoji_e34d, R.drawable.emoji_e339,
                R.drawable.emoji_e147, R.drawable.emoji_e343, R.drawable.emoji_e33c, R.drawable.emoji_e33a,
                R.drawable.emoji_e43f, R.drawable.emoji_e34b, R.drawable.emoji_e046, R.drawable.emoji_e345,
                R.drawable.emoji_e346, R.drawable.emoji_e348, R.drawable.emoji_e347, R.drawable.emoji_e34a,
                R.drawable.emoji_e349, R.drawable.emoji_e036, R.drawable.emoji_e157, R.drawable.emoji_e038,
                R.drawable.emoji_e153, R.drawable.emoji_e155, R.drawable.emoji_e14d, R.drawable.emoji_e156,
                R.drawable.emoji_e501, R.drawable.emoji_e158, R.drawable.emoji_e43d, R.drawable.emoji_e037,
                R.drawable.emoji_e504, R.drawable.emoji_e44a, R.drawable.emoji_e146, R.drawable.emoji_e50a,
                R.drawable.emoji_e505, R.drawable.emoji_e506, R.drawable.emoji_e122, R.drawable.emoji_e508,
                R.drawable.emoji_e509, R.drawable.emoji_e03b, R.drawable.emoji_e04d, R.drawable.emoji_e449,
                R.drawable.emoji_e44b, R.drawable.emoji_e51d, R.drawable.emoji_e44c, R.drawable.emoji_e124,
                R.drawable.emoji_e121, R.drawable.emoji_e433, R.drawable.emoji_e202, R.drawable.emoji_e135,
                R.drawable.emoji_e01c, R.drawable.emoji_e01d, R.drawable.emoji_e10d, R.drawable.emoji_e136,
                R.drawable.emoji_e42e, R.drawable.emoji_e01b, R.drawable.emoji_e15a, R.drawable.emoji_e159,
                R.drawable.emoji_e432, R.drawable.emoji_e430, R.drawable.emoji_e431, R.drawable.emoji_e42f,
                R.drawable.emoji_e01e, R.drawable.emoji_e039, R.drawable.emoji_e435, R.drawable.emoji_e01f,
                R.drawable.emoji_e125, R.drawable.emoji_e03a, R.drawable.emoji_e14e, R.drawable.emoji_e252,
                R.drawable.emoji_e137, R.drawable.emoji_e209, R.drawable.emoji_e154, R.drawable.emoji_e133,
                R.drawable.emoji_e150, R.drawable.emoji_e320, R.drawable.emoji_e123, R.drawable.emoji_e132,
                R.drawable.emoji_e143, R.drawable.emoji_e50b, R.drawable.emoji_e514, R.drawable.emoji_e513,
                R.drawable.emoji_e50c, R.drawable.emoji_e50d, R.drawable.emoji_e511, R.drawable.emoji_e50f,
                R.drawable.emoji_e512, R.drawable.emoji_e510, R.drawable.emoji_e50e, R.drawable.emoji_e21c,
                R.drawable.emoji_e21d, R.drawable.emoji_e21e, R.drawable.emoji_e21f, R.drawable.emoji_e220,
                R.drawable.emoji_e221, R.drawable.emoji_e222, R.drawable.emoji_e223, R.drawable.emoji_e224,
                R.drawable.emoji_e225, R.drawable.emoji_e210, R.drawable.emoji_e232, R.drawable.emoji_e233,
                R.drawable.emoji_e235, R.drawable.emoji_e234, R.drawable.emoji_e236, R.drawable.emoji_e237,
                R.drawable.emoji_e238, R.drawable.emoji_e239, R.drawable.emoji_e23b, R.drawable.emoji_e23a,
                R.drawable.emoji_e23d, R.drawable.emoji_e23c, R.drawable.emoji_e24d, R.drawable.emoji_e212,
                R.drawable.emoji_e24c, R.drawable.emoji_e213, R.drawable.emoji_e214, R.drawable.emoji_e507,
                R.drawable.emoji_e203, R.drawable.emoji_e20b, R.drawable.emoji_e22a, R.drawable.emoji_e22b,
                R.drawable.emoji_e226, R.drawable.emoji_e227, R.drawable.emoji_e22c, R.drawable.emoji_e22d,
                R.drawable.emoji_e215, R.drawable.emoji_e216, R.drawable.emoji_e217, R.drawable.emoji_e218,
                R.drawable.emoji_e228, R.drawable.emoji_e151, R.drawable.emoji_e138, R.drawable.emoji_e139,
                R.drawable.emoji_e13a, R.drawable.emoji_e208, R.drawable.emoji_e14f, R.drawable.emoji_e20a,
                R.drawable.emoji_e434, R.drawable.emoji_e309, R.drawable.emoji_e315, R.drawable.emoji_e30d,
                R.drawable.emoji_e207, R.drawable.emoji_e229, R.drawable.emoji_e206, R.drawable.emoji_e205,
                R.drawable.emoji_e204, R.drawable.emoji_e12e, R.drawable.emoji_e250, R.drawable.emoji_e251,
                R.drawable.emoji_e14a, R.drawable.emoji_e149, R.drawable.emoji_e23f, R.drawable.emoji_e240,
                R.drawable.emoji_e241, R.drawable.emoji_e242, R.drawable.emoji_e243, R.drawable.emoji_e244,
                R.drawable.emoji_e245, R.drawable.emoji_e246, R.drawable.emoji_e247, R.drawable.emoji_e248,
                R.drawable.emoji_e249, R.drawable.emoji_e24a, R.drawable.emoji_e24b, R.drawable.emoji_e23e,
                R.drawable.emoji_e532, R.drawable.emoji_e533, R.drawable.emoji_e534, R.drawable.emoji_e535,
                R.drawable.emoji_e21a, R.drawable.emoji_e219, R.drawable.emoji_e21b, R.drawable.emoji_e02f,
                R.drawable.emoji_e024, R.drawable.emoji_e025, R.drawable.emoji_e026, R.drawable.emoji_e027,
                R.drawable.emoji_e028, R.drawable.emoji_e029, R.drawable.emoji_e02a, R.drawable.emoji_e02b,
                R.drawable.emoji_e02c, R.drawable.emoji_e02d, R.drawable.emoji_e02e, R.drawable.emoji_e332,
                R.drawable.emoji_e333, R.drawable.emoji_e24e, R.drawable.emoji_e24f, R.drawable.emoji_e537 };

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    // NOTE: if you change anything about this array, you must make the
    // corresponding change
    // to the string arrays: default_smiley_texts and default_smiley_names in
    // res/values/arrays.xml
    public static final int[] DEFAULT_EMOJI_RES_IDS = { Emojis.getSmileyResource(0), Emojis.getSmileyResource(1),
            Emojis.getSmileyResource(2), Emojis.getSmileyResource(3), Emojis.getSmileyResource(4),
            Emojis.getSmileyResource(5), Emojis.getSmileyResource(6), Emojis.getSmileyResource(7),
            Emojis.getSmileyResource(8), Emojis.getSmileyResource(9), Emojis.getSmileyResource(10),
            Emojis.getSmileyResource(11), Emojis.getSmileyResource(12), Emojis.getSmileyResource(13),
            Emojis.getSmileyResource(14), Emojis.getSmileyResource(15), Emojis.getSmileyResource(16),
            Emojis.getSmileyResource(17), Emojis.getSmileyResource(18), Emojis.getSmileyResource(19),
            Emojis.getSmileyResource(20), Emojis.getSmileyResource(21), Emojis.getSmileyResource(22),
            Emojis.getSmileyResource(23), Emojis.getSmileyResource(24), Emojis.getSmileyResource(25),
            Emojis.getSmileyResource(26), Emojis.getSmileyResource(27), Emojis.getSmileyResource(28),
            Emojis.getSmileyResource(29), Emojis.getSmileyResource(30), Emojis.getSmileyResource(31),
            Emojis.getSmileyResource(32), Emojis.getSmileyResource(33), Emojis.getSmileyResource(34),
            Emojis.getSmileyResource(35), Emojis.getSmileyResource(36), Emojis.getSmileyResource(37),
            Emojis.getSmileyResource(38), Emojis.getSmileyResource(39), Emojis.getSmileyResource(40),
            Emojis.getSmileyResource(41), Emojis.getSmileyResource(42), Emojis.getSmileyResource(43),
            Emojis.getSmileyResource(44), Emojis.getSmileyResource(45), Emojis.getSmileyResource(46),
            Emojis.getSmileyResource(47), Emojis.getSmileyResource(48), Emojis.getSmileyResource(49),
            Emojis.getSmileyResource(50), Emojis.getSmileyResource(51), Emojis.getSmileyResource(52),
            Emojis.getSmileyResource(53), Emojis.getSmileyResource(54), Emojis.getSmileyResource(55),
            Emojis.getSmileyResource(56), Emojis.getSmileyResource(57), Emojis.getSmileyResource(58),
            Emojis.getSmileyResource(59), Emojis.getSmileyResource(60), Emojis.getSmileyResource(61),
            Emojis.getSmileyResource(62), Emojis.getSmileyResource(63), Emojis.getSmileyResource(64),
            Emojis.getSmileyResource(65), Emojis.getSmileyResource(66), Emojis.getSmileyResource(67),
            Emojis.getSmileyResource(68), Emojis.getSmileyResource(69), Emojis.getSmileyResource(70),
            Emojis.getSmileyResource(71), Emojis.getSmileyResource(72), Emojis.getSmileyResource(73),
            Emojis.getSmileyResource(74), Emojis.getSmileyResource(75), Emojis.getSmileyResource(76),
            Emojis.getSmileyResource(77), Emojis.getSmileyResource(78), Emojis.getSmileyResource(79),
            Emojis.getSmileyResource(80), Emojis.getSmileyResource(81), Emojis.getSmileyResource(82),
            Emojis.getSmileyResource(83), Emojis.getSmileyResource(84), Emojis.getSmileyResource(85),
            Emojis.getSmileyResource(86), Emojis.getSmileyResource(87), Emojis.getSmileyResource(88),
            Emojis.getSmileyResource(89), Emojis.getSmileyResource(90), Emojis.getSmileyResource(91),
            Emojis.getSmileyResource(92), Emojis.getSmileyResource(93), Emojis.getSmileyResource(94),
            Emojis.getSmileyResource(95), Emojis.getSmileyResource(96), Emojis.getSmileyResource(97),
            Emojis.getSmileyResource(98), Emojis.getSmileyResource(99), Emojis.getSmileyResource(100),
            Emojis.getSmileyResource(101), Emojis.getSmileyResource(102), Emojis.getSmileyResource(103),
            Emojis.getSmileyResource(104), Emojis.getSmileyResource(105), Emojis.getSmileyResource(106),
            Emojis.getSmileyResource(107), Emojis.getSmileyResource(108), Emojis.getSmileyResource(109),
            Emojis.getSmileyResource(110), Emojis.getSmileyResource(111), Emojis.getSmileyResource(112),
            Emojis.getSmileyResource(113), Emojis.getSmileyResource(114), Emojis.getSmileyResource(115),
            Emojis.getSmileyResource(116), Emojis.getSmileyResource(117), Emojis.getSmileyResource(118),
            Emojis.getSmileyResource(119), Emojis.getSmileyResource(120), Emojis.getSmileyResource(121),
            Emojis.getSmileyResource(122), Emojis.getSmileyResource(123), Emojis.getSmileyResource(124),
            Emojis.getSmileyResource(125), Emojis.getSmileyResource(126), Emojis.getSmileyResource(127),
            Emojis.getSmileyResource(128), Emojis.getSmileyResource(129), Emojis.getSmileyResource(130),
            Emojis.getSmileyResource(131), Emojis.getSmileyResource(132), Emojis.getSmileyResource(133),
            Emojis.getSmileyResource(134), Emojis.getSmileyResource(135), Emojis.getSmileyResource(136),
            Emojis.getSmileyResource(137), Emojis.getSmileyResource(138), Emojis.getSmileyResource(139),
            Emojis.getSmileyResource(140), Emojis.getSmileyResource(141), Emojis.getSmileyResource(142),
            Emojis.getSmileyResource(143), Emojis.getSmileyResource(144), Emojis.getSmileyResource(145),
            Emojis.getSmileyResource(146), Emojis.getSmileyResource(147), Emojis.getSmileyResource(148),
            Emojis.getSmileyResource(149), Emojis.getSmileyResource(150), Emojis.getSmileyResource(151),
            Emojis.getSmileyResource(152), Emojis.getSmileyResource(153), Emojis.getSmileyResource(154),
            Emojis.getSmileyResource(155), Emojis.getSmileyResource(156), Emojis.getSmileyResource(157),
            Emojis.getSmileyResource(158), Emojis.getSmileyResource(159), Emojis.getSmileyResource(160),
            Emojis.getSmileyResource(161), Emojis.getSmileyResource(162), Emojis.getSmileyResource(163),
            Emojis.getSmileyResource(164), Emojis.getSmileyResource(165), Emojis.getSmileyResource(166),
            Emojis.getSmileyResource(167), Emojis.getSmileyResource(168), Emojis.getSmileyResource(169),
            Emojis.getSmileyResource(170), Emojis.getSmileyResource(171), Emojis.getSmileyResource(172),
            Emojis.getSmileyResource(173), Emojis.getSmileyResource(174), Emojis.getSmileyResource(175),
            Emojis.getSmileyResource(176), Emojis.getSmileyResource(177), Emojis.getSmileyResource(178),
            Emojis.getSmileyResource(179), Emojis.getSmileyResource(180), Emojis.getSmileyResource(181),
            Emojis.getSmileyResource(182), Emojis.getSmileyResource(183), Emojis.getSmileyResource(184),
            Emojis.getSmileyResource(185), Emojis.getSmileyResource(186), Emojis.getSmileyResource(187),
            Emojis.getSmileyResource(188), Emojis.getSmileyResource(189), Emojis.getSmileyResource(190),
            Emojis.getSmileyResource(191), Emojis.getSmileyResource(192), Emojis.getSmileyResource(193),
            Emojis.getSmileyResource(194), Emojis.getSmileyResource(195), Emojis.getSmileyResource(196),
            Emojis.getSmileyResource(197), Emojis.getSmileyResource(198), Emojis.getSmileyResource(199),
            Emojis.getSmileyResource(200), Emojis.getSmileyResource(201), Emojis.getSmileyResource(202),
            Emojis.getSmileyResource(203), Emojis.getSmileyResource(204), Emojis.getSmileyResource(205),
            Emojis.getSmileyResource(206), Emojis.getSmileyResource(207), Emojis.getSmileyResource(208),
            Emojis.getSmileyResource(209), Emojis.getSmileyResource(210), Emojis.getSmileyResource(211),
            Emojis.getSmileyResource(212), Emojis.getSmileyResource(213), Emojis.getSmileyResource(214),
            Emojis.getSmileyResource(215), Emojis.getSmileyResource(216), Emojis.getSmileyResource(217),
            Emojis.getSmileyResource(218), Emojis.getSmileyResource(219), Emojis.getSmileyResource(220),
            Emojis.getSmileyResource(221), Emojis.getSmileyResource(222), Emojis.getSmileyResource(223),
            Emojis.getSmileyResource(224), Emojis.getSmileyResource(225), Emojis.getSmileyResource(226),
            Emojis.getSmileyResource(227), Emojis.getSmileyResource(228), Emojis.getSmileyResource(229),
            Emojis.getSmileyResource(230), Emojis.getSmileyResource(231), Emojis.getSmileyResource(232),
            Emojis.getSmileyResource(233), Emojis.getSmileyResource(234), Emojis.getSmileyResource(235),
            Emojis.getSmileyResource(236), Emojis.getSmileyResource(237), Emojis.getSmileyResource(238),
            Emojis.getSmileyResource(239), Emojis.getSmileyResource(240), Emojis.getSmileyResource(241),
            Emojis.getSmileyResource(242), Emojis.getSmileyResource(243), Emojis.getSmileyResource(244),
            Emojis.getSmileyResource(245), Emojis.getSmileyResource(246), Emojis.getSmileyResource(247),
            Emojis.getSmileyResource(248), Emojis.getSmileyResource(249), Emojis.getSmileyResource(250),
            Emojis.getSmileyResource(251), Emojis.getSmileyResource(252), Emojis.getSmileyResource(253),
            Emojis.getSmileyResource(254), Emojis.getSmileyResource(255), Emojis.getSmileyResource(256),
            Emojis.getSmileyResource(257), Emojis.getSmileyResource(258), Emojis.getSmileyResource(259),
            Emojis.getSmileyResource(260), Emojis.getSmileyResource(261), Emojis.getSmileyResource(262),
            Emojis.getSmileyResource(263), Emojis.getSmileyResource(264), Emojis.getSmileyResource(265),
            Emojis.getSmileyResource(266), Emojis.getSmileyResource(267), Emojis.getSmileyResource(268),
            Emojis.getSmileyResource(269), Emojis.getSmileyResource(270), Emojis.getSmileyResource(271),
            Emojis.getSmileyResource(272), Emojis.getSmileyResource(273), Emojis.getSmileyResource(274),
            Emojis.getSmileyResource(275), Emojis.getSmileyResource(276), Emojis.getSmileyResource(277),
            Emojis.getSmileyResource(278), Emojis.getSmileyResource(279), Emojis.getSmileyResource(280),
            Emojis.getSmileyResource(281), Emojis.getSmileyResource(282), Emojis.getSmileyResource(283),
            Emojis.getSmileyResource(284), Emojis.getSmileyResource(285), Emojis.getSmileyResource(286),
            Emojis.getSmileyResource(287), Emojis.getSmileyResource(288), Emojis.getSmileyResource(289),
            Emojis.getSmileyResource(290), Emojis.getSmileyResource(291), Emojis.getSmileyResource(292),
            Emojis.getSmileyResource(293), Emojis.getSmileyResource(294), Emojis.getSmileyResource(295),
            Emojis.getSmileyResource(296), Emojis.getSmileyResource(297), Emojis.getSmileyResource(298),
            Emojis.getSmileyResource(299), Emojis.getSmileyResource(300), Emojis.getSmileyResource(301),
            Emojis.getSmileyResource(302), Emojis.getSmileyResource(303), Emojis.getSmileyResource(304),
            Emojis.getSmileyResource(305), Emojis.getSmileyResource(306), Emojis.getSmileyResource(307),
            Emojis.getSmileyResource(308), Emojis.getSmileyResource(309), Emojis.getSmileyResource(310),
            Emojis.getSmileyResource(311), Emojis.getSmileyResource(312), Emojis.getSmileyResource(313),
            Emojis.getSmileyResource(314), Emojis.getSmileyResource(315), Emojis.getSmileyResource(316),
            Emojis.getSmileyResource(317), Emojis.getSmileyResource(318), Emojis.getSmileyResource(319),
            Emojis.getSmileyResource(320), Emojis.getSmileyResource(321), Emojis.getSmileyResource(322),
            Emojis.getSmileyResource(323), Emojis.getSmileyResource(324), Emojis.getSmileyResource(325),
            Emojis.getSmileyResource(326), Emojis.getSmileyResource(327), Emojis.getSmileyResource(328),
            Emojis.getSmileyResource(329), Emojis.getSmileyResource(330), Emojis.getSmileyResource(331),
            Emojis.getSmileyResource(332), Emojis.getSmileyResource(333), Emojis.getSmileyResource(334),
            Emojis.getSmileyResource(335), Emojis.getSmileyResource(336), Emojis.getSmileyResource(337),
            Emojis.getSmileyResource(338), Emojis.getSmileyResource(339), Emojis.getSmileyResource(340),
            Emojis.getSmileyResource(341), Emojis.getSmileyResource(342), Emojis.getSmileyResource(343),
            Emojis.getSmileyResource(344), Emojis.getSmileyResource(345), Emojis.getSmileyResource(346),
            Emojis.getSmileyResource(347), Emojis.getSmileyResource(348), Emojis.getSmileyResource(349),
            Emojis.getSmileyResource(350), Emojis.getSmileyResource(351), Emojis.getSmileyResource(352),
            Emojis.getSmileyResource(353), Emojis.getSmileyResource(354), Emojis.getSmileyResource(355),
            Emojis.getSmileyResource(356), Emojis.getSmileyResource(357), Emojis.getSmileyResource(358),
            Emojis.getSmileyResource(359), Emojis.getSmileyResource(360), Emojis.getSmileyResource(361),
            Emojis.getSmileyResource(362), Emojis.getSmileyResource(363), Emojis.getSmileyResource(364),
            Emojis.getSmileyResource(365), Emojis.getSmileyResource(366), Emojis.getSmileyResource(367),
            Emojis.getSmileyResource(368), Emojis.getSmileyResource(369), Emojis.getSmileyResource(370),
            Emojis.getSmileyResource(371), Emojis.getSmileyResource(372), Emojis.getSmileyResource(373),
            Emojis.getSmileyResource(374), Emojis.getSmileyResource(375), Emojis.getSmileyResource(376),
            Emojis.getSmileyResource(377), Emojis.getSmileyResource(378), Emojis.getSmileyResource(379),
            Emojis.getSmileyResource(380), Emojis.getSmileyResource(381), Emojis.getSmileyResource(382),
            Emojis.getSmileyResource(383), Emojis.getSmileyResource(384), Emojis.getSmileyResource(385),
            Emojis.getSmileyResource(386), Emojis.getSmileyResource(387), Emojis.getSmileyResource(388),
            Emojis.getSmileyResource(389), Emojis.getSmileyResource(390), Emojis.getSmileyResource(391),
            Emojis.getSmileyResource(392), Emojis.getSmileyResource(393), Emojis.getSmileyResource(394),
            Emojis.getSmileyResource(395), Emojis.getSmileyResource(396), Emojis.getSmileyResource(397),
            Emojis.getSmileyResource(398), Emojis.getSmileyResource(399), Emojis.getSmileyResource(400),
            Emojis.getSmileyResource(401), Emojis.getSmileyResource(402), Emojis.getSmileyResource(403),
            Emojis.getSmileyResource(404), Emojis.getSmileyResource(405), Emojis.getSmileyResource(406),
            Emojis.getSmileyResource(407), Emojis.getSmileyResource(408), Emojis.getSmileyResource(409),
            Emojis.getSmileyResource(410), Emojis.getSmileyResource(411), Emojis.getSmileyResource(412),
            Emojis.getSmileyResource(413), Emojis.getSmileyResource(414), Emojis.getSmileyResource(415),
            Emojis.getSmileyResource(416), Emojis.getSmileyResource(417), Emojis.getSmileyResource(418),
            Emojis.getSmileyResource(419), Emojis.getSmileyResource(420), Emojis.getSmileyResource(421),
            Emojis.getSmileyResource(422), Emojis.getSmileyResource(423), Emojis.getSmileyResource(424),
            Emojis.getSmileyResource(425), Emojis.getSmileyResource(426), Emojis.getSmileyResource(427),
            Emojis.getSmileyResource(428), Emojis.getSmileyResource(429), Emojis.getSmileyResource(430),
            Emojis.getSmileyResource(431), Emojis.getSmileyResource(432), Emojis.getSmileyResource(433),
            Emojis.getSmileyResource(434), Emojis.getSmileyResource(435), Emojis.getSmileyResource(436),
            Emojis.getSmileyResource(437), Emojis.getSmileyResource(438), Emojis.getSmileyResource(439),
            Emojis.getSmileyResource(440), Emojis.getSmileyResource(441), Emojis.getSmileyResource(442),
            Emojis.getSmileyResource(443), Emojis.getSmileyResource(444), Emojis.getSmileyResource(445),
            Emojis.getSmileyResource(446), Emojis.getSmileyResource(447), Emojis.getSmileyResource(448),
            Emojis.getSmileyResource(449), Emojis.getSmileyResource(450), Emojis.getSmileyResource(451),
            Emojis.getSmileyResource(452), Emojis.getSmileyResource(453), Emojis.getSmileyResource(454),
            Emojis.getSmileyResource(455), Emojis.getSmileyResource(456), Emojis.getSmileyResource(457),
            Emojis.getSmileyResource(458), Emojis.getSmileyResource(459), Emojis.getSmileyResource(460),
            Emojis.getSmileyResource(461), Emojis.getSmileyResource(462), Emojis.getSmileyResource(463),
            Emojis.getSmileyResource(464), Emojis.getSmileyResource(465), Emojis.getSmileyResource(466),
            Emojis.getSmileyResource(467), Emojis.getSmileyResource(468), Emojis.getSmileyResource(469),
            Emojis.getSmileyResource(470) };

    public static final int DEFAULT_EMOJI_TEXTS = R.array.default_emoji_texts;
    //public static final int DEFAULT_EMOJI_NAMES = R.array.default_emoji_names;

    /**
     * Builds the hashtable we use for mapping the string version of a smiley
     * (e.g. ":-)") to a resource ID for the icon version.
     */
    private HashMap<String, Integer> buildSmileyToRes() {
        if (DEFAULT_EMOJI_RES_IDS.length != mSmileyTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch - " + DEFAULT_EMOJI_RES_IDS.length
                    + " != " + mSmileyTexts.length);
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mSmileyTexts.length);
        for (int i = 0; i < mSmileyTexts.length; i++) {
            smileyToRes.put(mSmileyTexts[i], DEFAULT_EMOJI_RES_IDS[i]);
        }

        return smileyToRes;
    }

    /**
     * Builds the regular expression we use to find smileys in
     * {@link #addSmileySpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that one emoji is
        // 1 Unicode character.
        StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 1);

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (String s : mSmileyTexts) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }

    /**
     * Adds ImageSpans to a CharSequence that replace textual emoticons such as
     * :-) with a graphical version.
     * 
     * @param text
     *            A CharSequence possibly containing emoticons
     * @return A CharSequence annotated with ImageSpans covering any recognized
     *         emoticons.
     */
    public CharSequence addSmileySpans(CharSequence text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        String values = "";
        for (int i = 0; i < text.length(); i++) {
            values += text.charAt(i) + "<>" + Integer.toHexString(text.charAt(i)) + " ";
        }

        Matcher matcher = mPattern.matcher(text);
        while (matcher.find()) {
            int resId = mSmileyToRes.get(matcher.group());
            builder.setSpan(new ImageSpan(mContext, resId), matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }
}
