package org.example.librarymanagement.service.impl;


import lombok.RequiredArgsConstructor;
import org.example.librarymanagement.dto.MemberRequestDto;
import org.example.librarymanagement.dto.MemberResponseDto;
import org.example.librarymanagement.dto.PageResponseDto;
import org.example.librarymanagement.entity.Member;
import org.example.librarymanagement.exception.DuplicateResourceException;
import org.example.librarymanagement.exception.ResourceNotFoundException;
import org.example.librarymanagement.repository.MemberRepository;
import org.example.librarymanagement.service.MemberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public MemberResponseDto create(MemberRequestDto dto) {
        memberRepository.findByEmail(dto.getEmail()).ifPresent(m -> {
            throw new DuplicateResourceException("Bu email artıq istifadə olunur: " + dto.getEmail());
        });

        Member member = Member.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .membershipDate(dto.getMembershipDate())
                .build();

        return toResponse(memberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponseDto getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<MemberResponseDto> getAll(Pageable pageable) {
        Page<MemberResponseDto> page = memberRepository.findAll(pageable).map(this::toResponse);
        return PageResponseDto.from(page);
    }

    @Override
    public MemberResponseDto update(Long id, MemberRequestDto dto) {
        Member member = findEntity(id);
        member.setFullName(dto.getFullName());
        member.setEmail(dto.getEmail());
        member.setMembershipDate(dto.getMembershipDate());
        return toResponse(memberRepository.save(member));
    }

    @Override
    public void delete(Long id) {
        Member member = findEntity(id);
        memberRepository.delete(member);
    }

    private Member findEntity(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Member", id));
    }

    private MemberResponseDto toResponse(Member member) {
        return MemberResponseDto.builder()
                .id(member.getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .membershipDate(member.getMembershipDate())
                .build();
    }
}
