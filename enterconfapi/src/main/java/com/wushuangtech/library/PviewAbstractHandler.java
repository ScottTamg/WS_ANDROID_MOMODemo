package com.wushuangtech.library;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

import com.wushuangtech.utils.PviewLog;

import java.util.ArrayList;
import java.util.List;


/**
 * Abstract handler.
 * <ul>
 * It used to handle message and handle time out message
 * </ul>
 * <ul>
 * Notice: If you want to override {@link #handleMessage(Message)}, please
 * handle time out message
 * </ul>
 * 
 * @author jiangzhen
 * 
 */
public abstract class PviewAbstractHandler extends Handler {

	protected static final int REQUEST_TIME_OUT = 0;

	protected static final int MONITOR_TYPE_CONFERENCE = 0X01000000;

	protected static final int MONITOR_TYPE_DEVICE = 0X02000000;

	protected static final int MONITOR_TYPE_CONTACT = 0X03000000;

	protected static final int DEFAULT_TIME_OUT_SECS = 10;

	private SparseArray<HandlerWrapWithTimeout> handlerWrapWithTimeouHolder = new SparseArray<HandlerWrapWithTimeout>();

	private SparseArray<List<HandlerWrap>> handlerWrapListHolder = new SparseArray<List<HandlerWrap>>();

	private SparseArray<List<PendingObject>> pendingObjectHolder = new SparseArray<List<PendingObject>>();

	public PviewAbstractHandler (Looper looper) {
		super(looper);
	}

	// -----------------直接通知上层-------------------------------------------------
	protected void callerSendMessage(HandlerWrap caller, Object obj) {
		if (caller != null) {
			Message result = Message.obtain();
			result.what = caller.getWhat();
			result.obj = obj;
			caller.getHandler().sendMessage(result);
		}
	}

	// ------------------带超时功能通知上层------------------------------------------------

	// 超时处理的消息
	protected Message initTimeoutMessage(int mointorMessageID, long timeOutSec,
			HandlerWrap caller) {
		// Create unique message object
		Message msg = Message.obtain(this, REQUEST_TIME_OUT, mointorMessageID,
				0, new Object());
		handlerWrapWithTimeouHolder.put(Integer.valueOf(mointorMessageID),
				new HandlerWrapWithTimeout(mointorMessageID, caller, msg));
		this.sendMessageDelayed(msg, timeOutSec * 1000);
		return msg;
	}

	private HandlerWrap removeTimeoutMessage(int mointorMessageID) {
		HandlerWrapWithTimeout meta = handlerWrapWithTimeouHolder.get(Integer
				.valueOf(mointorMessageID));
		handlerWrapWithTimeouHolder.remove(Integer.valueOf(mointorMessageID));
		if (meta != null) {
			this.removeMessages(REQUEST_TIME_OUT, meta.timeoutMessage.obj);
			return meta.caller;
		} else {
			return null;
		}
	}

	@Override
	public void handleMessage(Message msg) {
		Message message = null;
		PviewLog.d(this.getClass().getName() + "   " + msg.what);
		switch (msg.what) {
		case REQUEST_TIME_OUT:
			// 返回上层超时，并把对应的监听者从列表中删除。
			Integer key = Integer.valueOf(msg.arg1);
			HandlerWrapWithTimeout timeMessageWrap = handlerWrapWithTimeouHolder
					.get(key);
			if (timeMessageWrap != null && timeMessageWrap.caller != null) {

				JNIResponse jniRes = new JNIResponse(
						JNIResponse.Result.TIME_OUT);

				jniRes.callerObject = timeMessageWrap.caller.getObject();
				if (timeMessageWrap.caller.getHandler() != null) {
					message = Message.obtain(
							timeMessageWrap.caller.getHandler(),
							timeMessageWrap.caller.getWhat(), jniRes);
				} else {
					PviewLog.w(" message no target:" + timeMessageWrap.caller);
				}
			} else {
				PviewLog.w("Doesn't find time out message in the queue :"
						+ msg.arg1);
			}
			// remove cache
			handlerWrapWithTimeouHolder.remove(key);
			break;
		default:
			// Handle normal message
			HandlerWrap handlerWrap = removeTimeoutMessage(msg.what);
			
			if (handlerWrap == null) {
				PviewLog.w(this.getClass().getName()
						+ " Igore message client don't expect callback :"
						+ msg.what);
				return;
			}
			Object origObject = handlerWrap.getObject();
			if (handlerWrap.getHandler() != null) {
				message = Message.obtain(handlerWrap.getHandler(),
						handlerWrap.getWhat());
				JNIResponse jniRes = (JNIResponse) msg.obj;
				jniRes.callerObject = origObject;
				message.obj = jniRes;
			} else {
				PviewLog.w("Doesn't find  message in the queue :" + msg.arg1);
			}
			break;
		}

		if (message == null) {
			PviewLog.w(" can not send message:" + msg.what
					+ " to target caller is null");
			return;
		} else {
			if (message.getTarget() == null) {
				PviewLog.w(" can not send message:" + msg.what
						+ " to target caller target(" + message.what
						+ ") is null");
				return;
			}
			message.sendToTarget();
		}
	}

	// --------------------长驻通知上层----------------------------------------------

	protected void registerListener(int key, Handler h, int what, Object obj) {
		synchronized (pendingObjectHolder) {
			List<HandlerWrap> list = handlerWrapListHolder.get(key);
			if (list == null) {
				list = new ArrayList<HandlerWrap>();
				handlerWrapListHolder.append(key, list);
			}
			HandlerWrap re = new HandlerWrap(h, what, obj);
			list.add(re);

			// 把缓存的该种消息发出去。
			List<PendingObject> pendingList = pendingObjectHolder.get(key);
			if (pendingList == null || pendingList.size() <= 0) {
				return;
			}

			for (int i = 0; i < pendingList.size(); i++) {
				PendingObject po = pendingList.get(i);
				Message.obtain(h, re.getWhat(), po.arg1, po.arg2,
						new AsyncResult(re.getObject(), po.obj)).sendToTarget();
			}

			pendingList.clear();
			pendingObjectHolder.remove(key);
		}
	}

	protected void unRegisterListener(int key, Handler h, int what, Object obj) {
		List<HandlerWrap> list = handlerWrapListHolder.get(key);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				HandlerWrap re = list.get(i);
				if (re.getHandler() == h && what == re.getWhat()) {
					list.remove(re);
					i--;
				}
			}
		}
	}

	/**
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @param obj
	 */
	protected void notifyListener(int key, int arg1, int arg2, Object obj) {
		List<HandlerWrap> handlerWrapList = handlerWrapListHolder.get(key);
		if (handlerWrapList == null || handlerWrapList.size() <= 0) {
			PviewLog.i(this.getClass().getName() + "  : No listener: " + key + " "
					+ arg1 + "  " + arg2 + "  " + obj);
			return;
		} else {
			PviewLog.i(this.getClass().getName() + "  : Notify listener: " + key
					+ " " + arg1 + "  " + arg2 + "  " + obj);
		}

		for (HandlerWrap messageListener : handlerWrapList) {
			Handler handler = messageListener.getHandler();
			if (handler != null) {
				Message.obtain(handler, messageListener.getWhat(), arg1, arg2,
						new AsyncResult(messageListener.getObject(), obj))
						.sendToTarget();
			}
		}
	}

	/**
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @param obj
	 */
	protected void notifyListenerWithPending(int key, int arg1, int arg2,
			Object obj) {
		List<HandlerWrap> list = handlerWrapListHolder.get(key);
		if (list == null || list.size() <= 0) {
			// 如果上层没有监听，就把消息缓存起来。再上层调用notifyListener时会把缓存的消息发出去。
			List<PendingObject> pendingList = pendingObjectHolder.get(key);
			if (pendingList == null) {
				pendingList = new ArrayList<PendingObject>();
				pendingObjectHolder.put(key, pendingList);
			}
			pendingList.add(new PendingObject(key, arg1, arg2, obj));
			PviewLog.i(this.getClass().getName() + "  : pend obj for " + key
					+ "  " + pendingList.size() + "   "
					+ pendingObjectHolder.size());
			return;
		} else {
			PviewLog.i(this.getClass().getName() + "  : Notify listener: " + key
					+ " " + arg1 + "  " + arg2 + "  " + obj);
			for (int i = 0; i < list.size(); i++) {
				HandlerWrap messageListener = list.get(i);
				Handler h = messageListener.getHandler();
				if (h != null) {
					Message.obtain(h, messageListener.getWhat(), arg1, arg2,
							new AsyncResult(messageListener.getObject(), obj))
							.sendToTarget();
				}
			}
		}

	}

	/**
	 * check parameters. Send incorrect response to caller
	 * 
	 * @param caller
	 * @param objs
	 * @return false means parameter is null otherwise true
	 */
	protected boolean checkParamNull(HandlerWrap caller, Object... objs) {
		boolean flag = false;
		for (Object obj : objs) {
			if (obj == null) {
				flag = true;
				break;
			}
		}
		if (flag && caller != null) {
			callerSendMessage(caller, new JNIResponse(
					JNIResponse.Result.INCORRECT_PAR));
			return false;
		}
		return true;
	}

	/**
	 * Clear all callbacks from JNI interface
	 */
	public abstract void clearCalledBack();

    class HandlerWrapWithTimeout {
		int mointorMessageID;
		HandlerWrap caller;
		Message timeoutMessage;

		public HandlerWrapWithTimeout(int mointorMessageID, HandlerWrap caller,
				Message timeoutMessage) {
			super();
			this.mointorMessageID = mointorMessageID;
			this.caller = caller;
			this.timeoutMessage = timeoutMessage;
		}

	}

	class PendingObject {
		int key;
		int arg1;
		int arg2;
		Object obj;

		public PendingObject(int key, int arg1, int arg2, Object obj) {
			super();
			this.key = key;
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.obj = obj;
		}

	}

}
