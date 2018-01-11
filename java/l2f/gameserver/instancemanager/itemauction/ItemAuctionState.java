/*	*/ package l2f.gameserver.instancemanager.itemauction;
/*	*/ 
/*	*/ import l2f.commons.lang.ArrayUtils;
/*	*/ 
/*	*/ public enum ItemAuctionState
/*	*/ {
/*  5 */   CREATED, STARTED, FINISHED;
/*	*/ 
/*	*/   public static final ItemAuctionState stateForStateId(int stateId)
/*	*/   {
/* 13 */	return ((ItemAuctionState)ArrayUtils.valid(values(), stateId));
/*	*/   }
/*	*/ }