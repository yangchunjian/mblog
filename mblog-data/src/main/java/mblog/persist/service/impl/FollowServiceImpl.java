/**
 *
 */
package mblog.persist.service.impl;

import mblog.data.User;
import mblog.data.UserFull;
import mblog.persist.dao.FollowDao;
import mblog.persist.entity.FollowPO;
import mblog.persist.entity.UserPO;
import mblog.persist.service.FollowService;
import mblog.persist.service.UserEventService;
import mblog.persist.utils.BeanMapUtils;
import mtons.modules.exception.MtonsException;
import mtons.modules.pojos.Paging;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author langhsu
 *
 */
public class FollowServiceImpl implements FollowService {
	@Autowired
	private FollowDao followDao;
	@Autowired
	private UserEventService userEventService;

	@Override
	@Transactional
	public long follow(long userId, long followId) {
		long ret = 0;

		Assert.state(userId != followId, "您不能关注自己");

		FollowPO po = followDao.checkFollow(userId, followId);

		if (po == null) {
			po = new FollowPO();
			po.setUser(new UserPO(userId));
			po.setFollow(new UserPO(followId));
			po.setCreated(new Date());

			followDao.save(po);

			ret = po.getId();

			userEventService.identityFollow(Collections.singletonList(userId), followId, true);
			userEventService.identityFans(Collections.singletonList(followId), userId, true);
		} else {
			throw new MtonsException("您已经关注过此用户了");
		}
		return ret;
	}

	@Override
	@Transactional
	public void unfollow(long userId, long followId) {
		int ret = followDao.unfollow(userId, followId);

		if (ret <= 0) {
			throw new MtonsException("取消关注失败");
		} else {
			userEventService.identityFollow(Collections.singletonList(userId), followId, false);
			userEventService.identityFans(Collections.singletonList(followId), userId, false);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void follows(Paging paging, long userId) {
		List<FollowPO> list = followDao.following(paging, userId);
		List<UserFull> rets = new ArrayList<>();

		for (FollowPO po : list) {
			UserFull u = new UserFull();
			BeanUtils.copyProperties(po.getFollow(), u, BeanMapUtils.USER_IGNORE);
			if (po.getFollow().getExtend() != null) {
				BeanUtils.copyProperties(po.getFollow().getExtend(), u);
			}
			rets.add(u);
		}
		paging.setResults(rets);
	}

	@Override
	@Transactional(readOnly = true)
	public void fans(Paging paging, long userId) {
		List<FollowPO> list = followDao.fans(paging, userId);
		List<UserFull> rets = new ArrayList<>();

		for (FollowPO po : list) {
			UserFull u = new UserFull();
			BeanUtils.copyProperties(po.getUser(), u, BeanMapUtils.USER_IGNORE);
			if (po.getUser().getExtend() != null) {
				BeanUtils.copyProperties(po.getUser().getExtend(), u);
			}
			rets.add(u);
		}
		paging.setResults(rets);
	}

	@Override
	@Transactional
	public boolean checkFollow(long userId, long followId) {
		return (followDao.checkFollow(userId, followId) != null);
	}

	@Override
	@Transactional
	public boolean checkCrossFollow(long userId, long targetUserId) {
		return followDao.checkCrossFollow(userId, targetUserId);
	}

}