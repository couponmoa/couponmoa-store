package com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.service;

import com.couponmoa.backend.couponmoastore.common.emailSender.dto.SendToMQDto;
import com.couponmoa.backend.couponmoastore.common.emailSender.service.SqsService;
import com.couponmoa.backend.couponmoastore.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;
import com.couponmoa.backend.couponmoastore.domain.store.grpc.UserGrpcClient;
import com.couponmoa.backend.couponmoastore.domain.store.repository.StoreRepository;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.dto.response.FindStoreSubscribeListResponse;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.entity.UserStoreSubscribe;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.repository.UserStoreSubscribeRepository;
import com.couponmoa.grpc.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.couponmoa.backend.couponmoastore.common.exception.ErrorCode.DUPLICATED_USER_COUPON;
import static com.couponmoa.backend.couponmoastore.common.exception.ErrorCode.STORE_NOT_FOUND;


@Service
@RequiredArgsConstructor
public class UserStoreSubscribeService {

    private final UserGrpcClient userGrpcClient;
    private final StoreRepository storeRepo;
    private final UserStoreSubscribeRepository userStoreSubRepo;
    private final SqsService sqsService;

    @Transactional
    public void subscribeStore(Long userId, Long storeId) {

        Store store = getStore(storeId);

        if (userStoreSubRepo.existsByUserIdAndStore(userId, store)) {
            throw new ApplicationException(DUPLICATED_USER_COUPON);
        }

        UserStoreSubscribe userCouponSubscribe = new UserStoreSubscribe(userId, store);
        userStoreSubRepo.save(userCouponSubscribe);
    }

    @Transactional
    public void unSubscribeCoupon(Long userId, Long storeId) {
        Store store = getStore(storeId);

        UserStoreSubscribe userCouponSubscribe = userStoreSubRepo.findByUserIdAndStore(userId, store).orElseThrow(() -> new ApplicationException(STORE_NOT_FOUND));

        userStoreSubRepo.delete(userCouponSubscribe);
    }

    @Transactional(readOnly = true)
    public List<FindStoreSubscribeListResponse> findSubscribeList(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return userStoreSubRepo.findByUserId(userId, pageable)
                .stream()
                .map(FindStoreSubscribeListResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> sendToSQS(Long storeId) {
        Store store = storeRepo.findByIdOrElseThrow(storeId, STORE_NOT_FOUND);

        List<String> emailList = userStoreSubRepo.findByStore_Id(storeId).stream()
                .map(UserStoreSubscribe::getUserId)
                .map(userGrpcClient::getUserById)
                .map(UserResponse::getEmail)
                .toList();

        if (emailList.isEmpty()) {
            return emailList;
        }

        SendToMQDto message = new SendToMQDto(
                emailList,
                "가게 새 쿠폰 발행 안내",
                store.getName(),
                "에서 새 쿠폰이 발행되었습니다!");

        sqsService.sendMessage(message);

        return emailList;
    }

    private Store getStore(Long storeId) {
        return storeRepo.findByIdOrElseThrow(storeId, STORE_NOT_FOUND);
    }
}
