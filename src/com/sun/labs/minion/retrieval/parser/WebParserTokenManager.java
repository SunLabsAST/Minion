/* Generated By:JJTree&JavaCC: Do not edit this line. WebParserTokenManager.java */
package com.sun.labs.minion.retrieval.parser;

public class WebParserTokenManager implements WebParserConstants
{
  public  java.io.PrintStream debugStream = System.out;
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_0(int pos, long active0)
{
   switch (pos)
   {
      default :
         return -1;
   }
}
private final int jjStartNfa_0(int pos, long active0)
{
   return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
}
private final int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private final int jjStartNfaWithStates_0(int pos, int kind, int state)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) { return pos + 1; }
   return jjMoveNfa_0(state, pos + 1);
}
private final int jjMoveStringLiteralDfa0_0()
{
   switch(curChar)
   {
      case 34:
         return jjStartNfaWithStates_0(0, 29, 21);
      case 43:
         return jjStartNfaWithStates_0(0, 25, 22);
      case 45:
         return jjStartNfaWithStates_0(0, 26, 22);
      case 47:
         return jjStartNfaWithStates_0(0, 27, 22);
      case 91:
         return jjStartNfaWithStates_0(0, 30, 23);
      case 93:
         return jjStartNfaWithStates_0(0, 31, 24);
      case 126:
         return jjStartNfaWithStates_0(0, 28, 24);
      case 160:
         return jjStopAtPos(0, 2);
      case 5760:
         return jjStopAtPos(0, 3);
      case 6158:
         return jjStopAtPos(0, 4);
      case 8192:
         return jjStopAtPos(0, 5);
      case 8193:
         return jjStopAtPos(0, 6);
      case 8194:
         return jjStopAtPos(0, 7);
      case 8195:
         return jjStopAtPos(0, 8);
      case 8196:
         return jjStopAtPos(0, 9);
      case 8197:
         return jjStopAtPos(0, 10);
      case 8198:
         return jjStopAtPos(0, 11);
      case 8199:
         return jjStopAtPos(0, 12);
      case 8200:
         return jjStopAtPos(0, 13);
      case 8201:
         return jjStopAtPos(0, 14);
      case 8202:
         return jjStopAtPos(0, 15);
      case 8203:
         return jjStopAtPos(0, 16);
      case 8232:
         return jjStopAtPos(0, 17);
      case 8233:
         return jjStopAtPos(0, 18);
      case 8239:
         return jjStopAtPos(0, 19);
      case 8287:
         return jjStopAtPos(0, 20);
      case 12288:
         return jjStopAtPos(0, 21);
      default :
         return jjMoveNfa_0(12, 0);
   }
}
private final void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private final void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private final void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}
private final void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}
private final void jjCheckNAddStates(int start)
{
   jjCheckNAdd(jjnextStates[start]);
   jjCheckNAdd(jjnextStates[start + 1]);
}
static final long[] jjbitVec0 = {
   0xfffe000000025040L, 0xffffffffffffffffL, 0xffffffffffffffffL, 0x3600000000ffffffL
};
static final long[] jjbitVec2 = {
   0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec3 = {
   0xffffffffffffffffL, 0xffffffffffffffffL, 0x0L, 0x0L
};
static final long[] jjbitVec4 = {
   0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0x0L
};
static final long[] jjbitVec5 = {
   0x0L, 0x0L, 0xffffffff00000000L, 0xffffffffffffffffL
};
static final long[] jjbitVec6 = {
   0xfffffffffffffffeL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec7 = {
   0x0L, 0xffffffffffff0000L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec8 = {
   0xffffffffffffffffL, 0xffff00000000ffffL, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec9 = {
   0x0L, 0xffffffff00000000L, 0xffffffffffffffffL, 0xffffffffL
};
static final long[] jjbitVec10 = {
   0x200002L, 0x0L, 0x0L, 0x0L
};
static final long[] jjbitVec11 = {
   0x0L, 0x0L, 0x420040000000000L, 0xff7fffffff7fffffL
};
static final long[] jjbitVec12 = {
   0x7fffffffffffffL, 0xffffffffffff0000L, 0xffffffffffffffffL, 0x401f0003ffc3L
};
static final long[] jjbitVec13 = {
   0x0L, 0x400000000000000L, 0xfffffffbffffd740L, 0xfbfffffffff7fffL
};
static final long[] jjbitVec14 = {
   0xffffffffffffffffL, 0xffffffffffffffffL, 0xfffffffffffffc03L, 0x33fffffffff7fffL
};
static final long[] jjbitVec15 = {
   0xfffe00000000ffffL, 0xfffffffe027fffffL, 0xffL, 0x707ffffff0000L
};
static final long[] jjbitVec16 = {
   0xfffffffd0000L, 0xe000L, 0x2003fffffffffL, 0x0L
};
static final long[] jjbitVec17 = {
   0x23fffffffffffff0L, 0xffc3ff010000L, 0x23c5fdfffff99fe0L, 0x3ffc3b0000000L
};
static final long[] jjbitVec18 = {
   0x36dfdfffff987e0L, 0x1cffc05e000000L, 0x23edfdfffffbbfe0L, 0xffc300010000L
};
static final long[] jjbitVec19 = {
   0x23edfdfffff99fe0L, 0x2ffc3b0000000L, 0x0L, 0x0L
};
static final long[] jjbitVec20 = {
   0x0L, 0x0L, 0x2ffbfffffc7fffe0L, 0x7fL
};
static final long[] jjbitVec21 = {
   0x6fbffffffffL, 0x3f03ffL, 0x0L, 0x0L
};
static final long[] jjbitVec22 = {
   0xffffffffffffff7fL, 0xffffffff3d7f3d7fL, 0x7f3d7fffffff3d7fL, 0xffff7fffff7f7f3dL
};
static final long[] jjbitVec23 = {
   0xffffffff7f3d7fffL, 0x3fe0007ffff7fL, 0xffffffff00000000L, 0x1fffffffffffffL
};
static final long[] jjbitVec24 = {
   0xffffffffffffffffL, 0x7f9fffffffffffL, 0xffffffff07fffffeL, 0x7ffffffffffL
};
static final long[] jjbitVec25 = {
   0x3ffff0003dfffL, 0x1dfff0003ffffL, 0xfffffffffffffL, 0x3ff10800000L
};
static final long[] jjbitVec26 = {
   0xffffffff03ff0000L, 0xffffffffffffffL, 0x1ffffffffffL, 0x0L
};
static final long[] jjbitVec27 = {
   0x1fffffffL, 0x1f3fffffffffc0L, 0x0L, 0x0L
};
static final long[] jjbitVec28 = {
   0xffffffffffffffffL, 0xfffffffffffL, 0x0L, 0x0L
};
static final long[] jjbitVec29 = {
   0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffff0fffffffL, 0x3ffffffffffffffL
};
static final long[] jjbitVec30 = {
   0xffffffff3f3fffffL, 0x3fffffffaaff3f3fL, 0x5fdfffffffffffffL, 0x1fdc1fff0fcf1fdcL
};
static final long[] jjbitVec31 = {
   0x0L, 0x8002000000000000L, 0x0L, 0x0L
};
static final long[] jjbitVec32 = {
   0xe3fbbd503e2ffc84L, 0x3e0L, 0x0L, 0x0L
};
static final long[] jjbitVec33 = {
   0x5f7ffdffa0f8007fL, 0xffdbL, 0x0L, 0x0L
};
static final long[] jjbitVec34 = {
   0x7fffffe03ff0000L, 0x7fffffeL, 0x0L, 0x0L
};
static final long[] jjbitVec35 = {
   0x0L, 0x0L, 0x100000000L, 0x0L
};
static final long[] jjbitVec36 = {
   0x0L, 0x0L, 0x1L, 0x0L
};
static final long[] jjbitVec37 = {
   0x4000L, 0x0L, 0x0L, 0x0L
};
static final long[] jjbitVec38 = {
   0x830000000fffL, 0x80000000L, 0x0L, 0x0L
};
static final long[] jjbitVec39 = {
   0x1L, 0x0L, 0x0L, 0x0L
};
static final long[] jjbitVec40 = {
   0x0L, 0x0L, 0x8880080200000000L, 0x0L
};
static final long[] jjbitVec41 = {
   0x0L, 0x4000000000000000L, 0x80L, 0x0L
};
static final long[] jjbitVec42 = {
   0x0L, 0xfc000000L, 0x4000000000000600L, 0x18000000000009L
};
static final long[] jjbitVec43 = {
   0x88003000L, 0x3c0000000000L, 0x0L, 0x100000L
};
static final long[] jjbitVec44 = {
   0x3fffL, 0x0L, 0x0L, 0x0L
};
static final long[] jjbitVec45 = {
   0x0L, 0x1003000000000L, 0x0L, 0x0L
};
static final long[] jjbitVec46 = {
   0x0L, 0x0L, 0x0L, 0x10000000000000L
};
static final long[] jjbitVec47 = {
   0x0L, 0xc008000L, 0x0L, 0x0L
};
static final long[] jjbitVec48 = {
   0x3c0000000007fff0L, 0x0L, 0x20L, 0x0L
};
static final long[] jjbitVec49 = {
   0x0L, 0xfc00L, 0x0L, 0x800000000000000L
};
static final long[] jjbitVec50 = {
   0x0L, 0x1fe00000000L, 0x0L, 0x0L
};
static final long[] jjbitVec51 = {
   0x0L, 0x600000000000L, 0x18000000L, 0x380000000000L
};
static final long[] jjbitVec52 = {
   0x60000000000000L, 0x0L, 0x0L, 0x7700000L
};
static final long[] jjbitVec53 = {
   0x7ffL, 0x0L, 0x0L, 0x0L
};
static final long[] jjbitVec54 = {
   0x0L, 0x30L, 0x0L, 0x0L
};
static final long[] jjbitVec55 = {
   0xffff00ffffff0000L, 0x60000000009bffefL, 0x6000L, 0x0L
};
static final long[] jjbitVec56 = {
   0x60000000000L, 0x0L, 0x70000000000000L, 0x0L
};
static final long[] jjbitVec57 = {
   0x0L, 0x3fff0000000000L, 0x0L, 0xfc000000000L
};
static final long[] jjbitVec58 = {
   0x0L, 0x0L, 0x1fffff8L, 0x300000000f000000L
};
static final long[] jjbitVec59 = {
   0x20010000fff3ff0eL, 0x0L, 0x100000000L, 0x800000000000000L
};
static final long[] jjbitVec60 = {
   0xc000000000000000L, 0x0L, 0x0L, 0x0L
};
static final long[] jjbitVec61 = {
   0xffff000000000000L, 0xd0bfff7ffffL, 0x0L, 0x0L
};
static final long[] jjbitVec62 = {
   0xb80000018c00f7eeL, 0x3fa8000000L, 0x0L, 0x0L
};
private final int jjMoveNfa_0(int startState, int curPos)
{
   int[] nextStates;
   int startsAt = 0;
   jjnewStateCnt = 21;
   int i = 1;
   jjstateSet[0] = startState;
   int j, kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 22:
                  if ((0xdc00fffa00000000L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if (curChar == 34)
                     jjCheckNAddStates(3, 6);
                  break;
               case 12:
                  if ((0xdc00fffa00000000L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 32)
                        kind = 32;
                     jjCheckNAddStates(7, 10);
                  }
                  else if (curChar == 34)
                     jjCheckNAddStates(3, 6);
                  if ((0xa80000000000L & l) != 0L)
                     jjAddStates(11, 12);
                  if ((0xa80000000000L & l) != 0L)
                     jjCheckNAddStates(0, 2);
                  break;
               case 24:
                  if ((0xdc00fffa00000000L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  break;
               case 23:
                  if ((0xdc00fffa00000000L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x100002600L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  if ((0xdc00fffa00000000L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  else if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  break;
               case 21:
                  if ((0xdc00fffa00000000L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  else if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  else if ((0x100002600L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  break;
               case 0:
                  if (curChar == 34)
                     jjCheckNAddStates(3, 6);
                  break;
               case 2:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  break;
               case 3:
                  if ((0x100002600L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  break;
               case 4:
                  if ((0xdc00fffa00000000L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  break;
               case 5:
                  if (curChar == 34 && kind > 33)
                     kind = 33;
                  break;
               case 8:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  break;
               case 9:
                  if ((0x100002600L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  break;
               case 10:
                  if ((0xdc00fffa00000000L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  break;
               case 14:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(0, 2);
                  break;
               case 15:
                  if ((0xdc00fffa00000000L & l) == 0L)
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(0, 2);
                  break;
               case 16:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 32)
                     kind = 32;
                  jjCheckNAddStates(7, 10);
                  break;
               case 17:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 38)
                     kind = 38;
                  jjCheckNAdd(17);
                  break;
               case 18:
                  if ((0xa80000000000L & l) != 0L)
                     jjAddStates(11, 12);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 22:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x78000001b8000001L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  if (curChar == 91)
                     jjCheckNAddStates(23, 26);
                  break;
               case 12:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                  {
                     if (kind > 32)
                        kind = 32;
                     jjCheckNAddStates(7, 10);
                  }
                  else if ((0x78000001b8000001L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  if (curChar == 126)
                     jjCheckNAddStates(0, 2);
                  else if (curChar == 91)
                     jjCheckNAddStates(23, 26);
                  break;
               case 24:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x78000001b8000001L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  break;
               case 23:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  else if ((0x78000001b8000001L & l) != 0L)
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  if ((0x7fffffe07fffffeL & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  else if ((0x78000001b8000001L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  break;
               case 21:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  else if ((0x78000001b8000001L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  break;
               case 2:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  break;
               case 4:
                  if ((0x78000001b8000001L & l) != 0L)
                     jjCheckNAddStates(18, 22);
                  break;
               case 6:
                  if (curChar == 91)
                     jjCheckNAddStates(23, 26);
                  break;
               case 8:
                  if ((0x7fffffe07fffffeL & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  break;
               case 10:
                  if ((0x78000001b8000001L & l) != 0L)
                     jjCheckNAddStates(13, 17);
                  break;
               case 11:
                  if (curChar == 93 && kind > 34)
                     kind = 34;
                  break;
               case 14:
                  if ((0x7fffffe07fffffeL & l) == 0L)
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(0, 2);
                  break;
               case 15:
                  if ((0x78000001b8000001L & l) == 0L)
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(0, 2);
                  break;
               case 16:
                  if ((0x7fffffe07fffffeL & l) == 0L)
                     break;
                  if (kind > 32)
                     kind = 32;
                  jjCheckNAddStates(7, 10);
                  break;
               case 17:
                  if ((0x7fffffe07fffffeL & l) == 0L)
                     break;
                  if (kind > 38)
                     kind = 38;
                  jjCheckNAdd(17);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int hiByte = (int)(curChar >> 8);
         int i1 = hiByte >> 6;
         long l1 = 1L << (hiByte & 077);
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         MatchLoop: do
         {
            switch(jjstateSet[--i])
            {
               case 22:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(27, 29);
                  }
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  break;
               case 12:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 32)
                        kind = 32;
                     jjCheckNAddStates(7, 10);
                  }
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(30, 33);
                  }
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  break;
               case 24:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(27, 29);
                  }
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  break;
               case 23:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(34, 38);
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(13, 17);
                  if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(13, 17);
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(13, 17);
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(27, 29);
                  }
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                  {
                     if (kind > 35)
                        kind = 35;
                     jjCheckNAddStates(0, 2);
                  }
                  break;
               case 21:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(39, 43);
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(18, 22);
                  if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(18, 22);
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(18, 22);
                  break;
               case 1:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(39, 43);
                  break;
               case 2:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(18, 22);
                  break;
               case 3:
                  if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(18, 22);
                  break;
               case 4:
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(18, 22);
                  break;
               case 7:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(34, 38);
                  break;
               case 8:
                  if (jjCanMove_1(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(13, 17);
                  break;
               case 9:
                  if (jjCanMove_2(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(13, 17);
                  break;
               case 10:
                  if (jjCanMove_3(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(13, 17);
                  break;
               case 13:
                  if (!jjCanMove_0(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(27, 29);
                  break;
               case 14:
                  if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(0, 2);
                  break;
               case 15:
                  if (!jjCanMove_3(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(0, 2);
                  break;
               case 16:
                  if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 32)
                     kind = 32;
                  jjCheckNAddStates(7, 10);
                  break;
               case 17:
                  if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 38)
                     kind = 38;
                  jjCheckNAdd(17);
                  break;
               case 19:
                  if (!jjCanMove_0(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 35)
                     kind = 35;
                  jjCheckNAddStates(30, 33);
                  break;
               case 20:
                  if (!jjCanMove_0(hiByte, i1, i2, l1, l2))
                     break;
                  if (kind > 49)
                     kind = 49;
                  jjCheckNAdd(20);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 21 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
static final int[] jjnextStates = {
   13, 14, 15, 1, 2, 3, 4, 13, 14, 15, 17, 0, 6, 7, 8, 9, 
   10, 11, 1, 2, 3, 4, 5, 7, 8, 9, 10, 14, 15, 13, 14, 15, 
   20, 13, 8, 9, 10, 11, 7, 2, 3, 4, 5, 1, 
};
private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 11:
         return ((jjbitVec2[i2] & l2) != 0L);
      case 13:
         return ((jjbitVec3[i2] & l2) != 0L);
      case 15:
         return ((jjbitVec4[i2] & l2) != 0L);
      case 16:
         return ((jjbitVec5[i2] & l2) != 0L);
      case 48:
         return ((jjbitVec6[i2] & l2) != 0L);
      case 251:
         return ((jjbitVec7[i2] & l2) != 0L);
      case 254:
         return ((jjbitVec8[i2] & l2) != 0L);
      case 255:
         return ((jjbitVec9[i2] & l2) != 0L);
      default : 
         if ((jjbitVec0[i1] & l1) != 0L)
            return true;
         return false;
   }
}
private static final boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 0:
         return ((jjbitVec11[i2] & l2) != 0L);
      case 2:
         return ((jjbitVec12[i2] & l2) != 0L);
      case 3:
         return ((jjbitVec13[i2] & l2) != 0L);
      case 4:
         return ((jjbitVec14[i2] & l2) != 0L);
      case 5:
         return ((jjbitVec15[i2] & l2) != 0L);
      case 7:
         return ((jjbitVec16[i2] & l2) != 0L);
      case 9:
         return ((jjbitVec17[i2] & l2) != 0L);
      case 10:
         return ((jjbitVec18[i2] & l2) != 0L);
      case 11:
         return ((jjbitVec19[i2] & l2) != 0L);
      case 13:
         return ((jjbitVec20[i2] & l2) != 0L);
      case 16:
         return ((jjbitVec21[i2] & l2) != 0L);
      case 18:
         return ((jjbitVec22[i2] & l2) != 0L);
      case 19:
         return ((jjbitVec23[i2] & l2) != 0L);
      case 20:
         return ((jjbitVec6[i2] & l2) != 0L);
      case 22:
         return ((jjbitVec24[i2] & l2) != 0L);
      case 23:
         return ((jjbitVec25[i2] & l2) != 0L);
      case 24:
         return ((jjbitVec26[i2] & l2) != 0L);
      case 25:
         return ((jjbitVec27[i2] & l2) != 0L);
      case 29:
         return ((jjbitVec28[i2] & l2) != 0L);
      case 30:
         return ((jjbitVec29[i2] & l2) != 0L);
      case 31:
         return ((jjbitVec30[i2] & l2) != 0L);
      case 32:
         return ((jjbitVec31[i2] & l2) != 0L);
      case 33:
         return ((jjbitVec32[i2] & l2) != 0L);
      case 251:
         return ((jjbitVec33[i2] & l2) != 0L);
      case 255:
         return ((jjbitVec34[i2] & l2) != 0L);
      default : 
         if ((jjbitVec10[i1] & l1) != 0L)
            return true;
         return false;
   }
}
private static final boolean jjCanMove_2(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 0:
         return ((jjbitVec35[i2] & l2) != 0L);
      case 22:
         return ((jjbitVec36[i2] & l2) != 0L);
      case 24:
         return ((jjbitVec37[i2] & l2) != 0L);
      case 32:
         return ((jjbitVec38[i2] & l2) != 0L);
      case 48:
         return ((jjbitVec39[i2] & l2) != 0L);
      default : 
         return false;
   }
}
private static final boolean jjCanMove_3(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 0:
         return ((jjbitVec40[i2] & l2) != 0L);
      case 3:
         return ((jjbitVec41[i2] & l2) != 0L);
      case 5:
         return ((jjbitVec42[i2] & l2) != 0L);
      case 6:
         return ((jjbitVec43[i2] & l2) != 0L);
      case 7:
         return ((jjbitVec44[i2] & l2) != 0L);
      case 9:
         return ((jjbitVec45[i2] & l2) != 0L);
      case 13:
         return ((jjbitVec46[i2] & l2) != 0L);
      case 14:
         return ((jjbitVec47[i2] & l2) != 0L);
      case 15:
         return ((jjbitVec48[i2] & l2) != 0L);
      case 16:
         return ((jjbitVec49[i2] & l2) != 0L);
      case 19:
         return ((jjbitVec50[i2] & l2) != 0L);
      case 22:
         return ((jjbitVec51[i2] & l2) != 0L);
      case 23:
         return ((jjbitVec52[i2] & l2) != 0L);
      case 24:
         return ((jjbitVec53[i2] & l2) != 0L);
      case 25:
         return ((jjbitVec54[i2] & l2) != 0L);
      case 32:
         return ((jjbitVec55[i2] & l2) != 0L);
      case 35:
         return ((jjbitVec56[i2] & l2) != 0L);
      case 39:
         return ((jjbitVec57[i2] & l2) != 0L);
      case 41:
         return ((jjbitVec58[i2] & l2) != 0L);
      case 48:
         return ((jjbitVec59[i2] & l2) != 0L);
      case 253:
         return ((jjbitVec60[i2] & l2) != 0L);
      case 254:
         return ((jjbitVec61[i2] & l2) != 0L);
      case 255:
         return ((jjbitVec62[i2] & l2) != 0L);
      default : 
         return false;
   }
}
public static final String[] jjstrLiteralImages = {
"", null, null, null, null, null, null, null, null, null, null, null, null, 
null, null, null, null, null, null, null, null, null, null, null, null, "\53", 
"\55", "\57", "\176", "\42", "\133", "\135", null, null, null, null, null, null, null, 
null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
null, };
public static final String[] lexStateNames = {
   "DEFAULT", 
};
static final long[] jjtoToken = {
   0xa00cffe000001L, 
};
static final long[] jjtoSkip = {
   0x1fffffeL, 
};
protected JavaCharStream input_stream;
private final int[] jjrounds = new int[21];
private final int[] jjstateSet = new int[42];
protected char curChar;
public WebParserTokenManager(JavaCharStream stream){
   if (JavaCharStream.staticFlag)
      throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
   input_stream = stream;
}
public WebParserTokenManager(JavaCharStream stream, int lexState){
   this(stream);
   SwitchTo(lexState);
}
public void ReInit(JavaCharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private final void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 21; i-- > 0;)
      jjrounds[i] = 0x80000000;
}
public void ReInit(JavaCharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}
public void SwitchTo(int lexState)
{
   if (lexState >= 1 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

protected Token jjFillToken()
{
   Token t = Token.newToken(jjmatchedKind);
   t.kind = jjmatchedKind;
   String im = jjstrLiteralImages[jjmatchedKind];
   t.image = (im == null) ? input_stream.GetImage() : im;
   t.beginLine = input_stream.getBeginLine();
   t.beginColumn = input_stream.getBeginColumn();
   t.endLine = input_stream.getEndLine();
   t.endColumn = input_stream.getEndColumn();
   return t;
}

int curLexState = 0;
int defaultLexState = 0;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

public Token getNextToken() 
{
  int kind;
  Token specialToken = null;
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {   
   try   
   {     
      curChar = input_stream.BeginToken();
   }     
   catch(java.io.IOException e)
   {        
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      return matchedToken;
   }

   try { input_stream.backup(0);
      while (curChar <= 32 && (0x100002600L & (1L << curChar)) != 0L)
         curChar = input_stream.BeginToken();
   }
   catch (java.io.IOException e1) { continue EOFLoop; }
   jjmatchedKind = 0x7fffffff;
   jjmatchedPos = 0;
   curPos = jjMoveStringLiteralDfa0_0();
   if (jjmatchedKind != 0x7fffffff)
   {
      if (jjmatchedPos + 1 < curPos)
         input_stream.backup(curPos - jjmatchedPos - 1);
      if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
      {
         matchedToken = jjFillToken();
         return matchedToken;
      }
      else
      {
         continue EOFLoop;
      }
   }
   int error_line = input_stream.getEndLine();
   int error_column = input_stream.getEndColumn();
   String error_after = null;
   boolean EOFSeen = false;
   try { input_stream.readChar(); input_stream.backup(1); }
   catch (java.io.IOException e1) {
      EOFSeen = true;
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
      if (curChar == '\n' || curChar == '\r') {
         error_line++;
         error_column = 0;
      }
      else
         error_column++;
   }
   if (!EOFSeen) {
      input_stream.backup(1);
      error_after = curPos <= 1 ? "" : input_stream.GetImage();
   }
   throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

}
