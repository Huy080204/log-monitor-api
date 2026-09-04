package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationQuery.NotificationQueryDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationQuery.CreateNotificationQueryForm;
import logs.api.form.notificationQuery.NotificationQueryLinkForm;
import logs.api.mapper.NotificationQueryMapper;
import logs.api.model.Applications;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationQuery;
import logs.api.model.QueryTemplate;
import logs.api.model.criteria.NotificationQueryCriteria;
import logs.api.repository.ApplicationsRepository;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.QueryTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryControllerTest {

    @Mock
    private NotificationQueryRepository notificationQueryRepository;
    @Mock
    private NotificationGroupRepository notificationGroupRepository;
    @Mock
    private QueryTemplateRepository queryTemplateRepository;
    @Mock
    private ApplicationsRepository applicationsRepository;
    @Mock
    private NotificationQueryMapper notificationQueryMapper;
    @InjectMocks
    private NotificationQueryController controller;

    private final BindingResult bindingResult = mock(BindingResult.class);

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnNotificationQueryDtoWhenGetIdExists() {
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto dto = new NotificationQueryDto();
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationQueryMapper.fromEntityToNotificationQueryDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationQueryDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get notification query success");
    }

    @Test
    void shouldReturnPagedListWhenListCalled() {
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto dto = new NotificationQueryDto();
        Page<NotificationQuery> page = new PageImpl<>(List.of(entity));
        when(notificationQueryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationQueryMapper.fromEntityListToNotificationQueryDtoList(List.of(entity))).thenReturn(List.of(dto));

        ApiMessageDto<ResponseListDto<List<NotificationQueryDto>>> result =
                controller.list(new NotificationQueryCriteria(), PageRequest.of(0, 10));

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).containsExactly(dto);
    }

    @Test
    void shouldAlwaysUseApplicationNameNullsLastSortWhenListCalled() {
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto dto = new NotificationQueryDto();
        Page<NotificationQuery> page = new PageImpl<>(List.of(entity));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationQueryRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(notificationQueryMapper.fromEntityListToNotificationQueryDtoList(List.of(entity))).thenReturn(List.of(dto));

        controller.list(new NotificationQueryCriteria(), PageRequest.of(0, 10));

        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(new Sort.Order(Sort.Direction.ASC, "queryTemplate.application.name").nullsLast()));
    }

    @Test
    void shouldIgnoreExplicitSortWhenListCalledWithSortParam() {
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto dto = new NotificationQueryDto();
        Page<NotificationQuery> page = new PageImpl<>(List.of(entity));
        Pageable requestedPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "id"));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(notificationQueryRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);
        when(notificationQueryMapper.fromEntityListToNotificationQueryDtoList(List.of(entity))).thenReturn(List.of(dto));

        controller.list(new NotificationQueryCriteria(), requestedPageable);

        Pageable actual = pageableCaptor.getValue();
        assertThat(actual.getPageNumber()).isEqualTo(1);
        assertThat(actual.getPageSize()).isEqualTo(5);
        assertThat(actual.getSort())
                .isEqualTo(Sort.by(new Sort.Order(Sort.Direction.ASC, "queryTemplate.application.name").nullsLast()));
    }

    private static NotificationQueryLinkForm link(Long queryTemplateId, Long applicationId) {
        NotificationQueryLinkForm link = new NotificationQueryLinkForm();
        link.setQueryTemplateId(queryTemplateId);
        link.setApplicationId(applicationId);
        return link;
    }

    @Test
    void shouldThrowNotFoundWhenCreateNotificationGroupDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(2L, 100L)));
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).saveAll(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
    }

    @Test
    void shouldThrowNotFoundWhenCreateQueryTemplateIdDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(2L, 100L), link(3L, 100L)));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        Applications a100 = new Applications();
        a100.setId(100L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t2));
        // matching-size stub so the applications check (if it runs before/after the template
        // check) doesn't mask this test's target NOT_FOUND — unused branch is harmless.
        lenient().when(applicationsRepository.findAllById(any())).thenReturn(List.of(a100));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).saveAll(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
    }

    @Test
    void shouldThrowBadRequestWhenCreateLinksHasDuplicatePair() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(5L, 100L), link(5L, 100L)));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Duplicate query template and application in request");

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).saveAll(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
    }

    @Test
    void shouldThrowNotFoundWhenCreateApplicationIdDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(2L, 100L)));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        // matching-size stub so the template check (if it runs before/after the applications
        // check) doesn't mask this test's target NOT_FOUND — unused branch is harmless.
        lenient().when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t2));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).saveAll(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
    }

    @Test
    void shouldThrowBadRequestWhenLinkApplicationMismatchesQueryTemplateApplication() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(2L, 200L)));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        Applications a100 = new Applications();
        a100.setId(100L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        t2.setApplication(a100);
        Applications a200 = new Applications();
        a200.setId(200L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t2));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of(a200));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_APPLICATION_MISMATCH);

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).saveAll(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveWhenLinkApplicationMatchesQueryTemplateApplication() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(2L, 100L)));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        Applications a100 = new Applications();
        a100.setId(100L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        t2.setApplication(a100);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t2));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of(a100));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of());

        ArgumentCaptor<List<NotificationQuery>> saveAllCaptor = ArgumentCaptor.forClass(List.class);

        ApiMessageDto<Void> result = controller.create(form, bindingResult);

        verify(notificationQueryRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(1);
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isNull();
        assertThat(result.getMessage()).isEqualTo("Create notification query success");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveBothRowsWhenGroupHasNoExistingQueries() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(10L, 100L), link(20L, 100L)));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t10 = new QueryTemplate();
        t10.setId(10L);
        QueryTemplate t20 = new QueryTemplate();
        t20.setId(20L);
        Applications a100 = new Applications();
        a100.setId(100L);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t10, t20));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of(a100));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of());

        ArgumentCaptor<List<NotificationQuery>> saveAllCaptor = ArgumentCaptor.forClass(List.class);

        ApiMessageDto<Void> result = controller.create(form, bindingResult);

        verify(notificationQueryRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(2);
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isNull();
        assertThat(result.getMessage()).isEqualTo("Create notification query success");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveDistinctRowsWhenSameTemplateDifferentApplication() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(1L, 100L), link(1L, 200L)));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t1 = new QueryTemplate();
        t1.setId(1L);
        Applications a100 = new Applications();
        a100.setId(100L);
        Applications a200 = new Applications();
        a200.setId(200L);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t1, t1));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of(a100, a200));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of());

        ArgumentCaptor<List<NotificationQuery>> saveAllCaptor = ArgumentCaptor.forClass(List.class);

        controller.create(form, bindingResult);

        verify(notificationQueryRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSyncQueriesByPairWhenGroupAlreadyLinkedToOtherTemplates() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(2L, 100L), link(3L, 100L)));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        QueryTemplate t3 = new QueryTemplate();
        t3.setId(3L);
        Applications a100 = new Applications();
        a100.setId(100L);

        // row2's pair (t2, a100) matches the first requested link exactly, so it's kept
        // untouched. rowLeftover belongs to a template/application pair not in this request's
        // links at all — it's the diff's delete side.
        QueryTemplate t5 = new QueryTemplate();
        t5.setId(5L);

        NotificationQuery row2 = new NotificationQuery();
        row2.setId(102L);
        row2.setNotificationGroup(group);
        row2.setQueryTemplate(t2);
        row2.setApplication(a100);
        row2.setStatus(BaseConstant.STATUS_PENDING);

        NotificationQuery rowLeftover = new NotificationQuery();
        rowLeftover.setId(105L);
        rowLeftover.setNotificationGroup(group);
        rowLeftover.setQueryTemplate(t5);
        rowLeftover.setApplication(a100);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t2, t3));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of(a100, a100));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of(row2, rowLeftover));

        ArgumentCaptor<Collection<NotificationQuery>> deleteAllCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<List<NotificationQuery>> saveAllCaptor = ArgumentCaptor.forClass(List.class);

        controller.create(form, bindingResult);

        verify(notificationQueryRepository).deleteAll(deleteAllCaptor.capture());
        assertThat(deleteAllCaptor.getValue()).containsExactly(rowLeftover);
        verify(notificationQueryRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(1);
        assertThat(saveAllCaptor.getValue().get(0).getQueryTemplate()).isSameAs(t3);
        assertThat(saveAllCaptor.getValue().get(0).getNotificationGroup()).isSameAs(group);
        // NotificationQuery inherits ReuseId's Lombok @Data equals() (compares only the
        // always-null `reusedId`), so an equals()-based contains/doesNotContain would be a
        // false positive here — assert by reference instead.
        assertThat(saveAllCaptor.getValue()).noneMatch(nq -> nq == row2);
        // status untouched — sync never re-saves a retained row, so PENDING must not be reset.
        assertThat(row2.getStatus()).isEqualTo(BaseConstant.STATUS_PENDING);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDeleteStaleApplicationPairAndInsertNewPairWhenSameTemplateLinksDifferentApplications() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(1L, 10L), link(1L, 30L)));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t1 = new QueryTemplate();
        t1.setId(1L);
        Applications a1 = new Applications();
        a1.setId(10L);
        Applications a2 = new Applications();
        a2.setId(20L);
        Applications a3 = new Applications();
        a3.setId(30L);

        // Existing rows: (t1, a1) and (t1, a2). Request keeps (t1, a1) and adds (t1, a3) —
        // (t1, a2) is stale and must be deleted, regression test for the bug where a
        // queryTemplateId-only match "kept" (t1, a2) and silently dropped (t1, a3).
        NotificationQuery rowA1 = new NotificationQuery();
        rowA1.setId(201L);
        rowA1.setNotificationGroup(group);
        rowA1.setQueryTemplate(t1);
        rowA1.setApplication(a1);

        NotificationQuery rowA2 = new NotificationQuery();
        rowA2.setId(202L);
        rowA2.setNotificationGroup(group);
        rowA2.setQueryTemplate(t1);
        rowA2.setApplication(a2);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t1));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of(a1, a3));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of(rowA1, rowA2));

        ArgumentCaptor<Collection<NotificationQuery>> deleteAllCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<List<NotificationQuery>> saveAllCaptor = ArgumentCaptor.forClass(List.class);

        controller.create(form, bindingResult);

        verify(notificationQueryRepository).deleteAll(deleteAllCaptor.capture());
        assertThat(deleteAllCaptor.getValue()).containsExactly(rowA2);
        verify(notificationQueryRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(1);
        assertThat(saveAllCaptor.getValue().get(0).getQueryTemplate()).isSameAs(t1);
        assertThat(saveAllCaptor.getValue().get(0).getApplication()).isSameAs(a3);
        assertThat(saveAllCaptor.getValue()).noneMatch(nq -> nq == rowA1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveDistinctRowWhenSameTemplateDifferentApplicationAlreadyLinked() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of(link(1L, 100L), link(1L, 200L)));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t1 = new QueryTemplate();
        t1.setId(1L);
        Applications a100 = new Applications();
        a100.setId(100L);
        Applications a200 = new Applications();
        a200.setId(200L);

        // Existing row's pair is (t1, a100) — matches the first requested link exactly, so it
        // must be kept untouched; only the (t1, a200) pair is new and gets inserted.
        NotificationQuery existingRow = new NotificationQuery();
        existingRow.setId(101L);
        existingRow.setNotificationGroup(group);
        existingRow.setQueryTemplate(t1);
        existingRow.setApplication(a100);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t1, t1));
        when(applicationsRepository.findAllById(any())).thenReturn(List.of(a100, a200));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of(existingRow));

        ArgumentCaptor<List<NotificationQuery>> saveAllCaptor = ArgumentCaptor.forClass(List.class);

        controller.create(form, bindingResult);

        verify(notificationQueryRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(1);
        assertThat(saveAllCaptor.getValue().get(0).getQueryTemplate()).isSameAs(t1);
        assertThat(saveAllCaptor.getValue().get(0).getApplication()).isSameAs(a200);
        assertThat(saveAllCaptor.getValue()).noneMatch(nq -> nq == existingRow);
        verify(notificationQueryRepository, never()).deleteAll(any());
    }

    @Test
    void shouldDeleteAllWhenLinksEmpty() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setLinks(List.of());

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        ApiMessageDto<Void> result = controller.create(form, bindingResult);

        verify(notificationQueryRepository).deleteAllByNotificationGroupId(1L);
        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).saveAll(any());
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isNull();
    }

    @Test
    void shouldThrowNotFoundWhenDeleteIdDoesNotExist() {
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND);
    }

    @Test
    void shouldDeleteWhenIdExists() {
        NotificationQuery entity = new NotificationQuery();
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete notification query success");
        verify(notificationQueryRepository).delete(entity);
    }
}
