package com.manuelfabri.expenses.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.RecurrentTransactionDto;
import com.manuelfabri.expenses.dto.RecurrentTransactionRequestDto;
import com.manuelfabri.expenses.service.RecurrentTransactionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping(Urls.RECURRENT_TRANSACTION)
public class RecurrentTransactionController {

  private RecurrentTransactionService recurrentTransactionService;

  public RecurrentTransactionController(RecurrentTransactionService recurrentTransactionService) {
    this.recurrentTransactionService = recurrentTransactionService;
  }

  @GetMapping
  public ResponseEntity<List<RecurrentTransactionDto>> getActiveForCurrentUser() {
    return new ResponseEntity<>(recurrentTransactionService.getActiveForCurrentUser(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecurrentTransactionDto> getById(@PathVariable Long id) {
    return new ResponseEntity<>(recurrentTransactionService.getById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<RecurrentTransactionDto> create(
      @RequestBody @Valid RecurrentTransactionRequestDto requestDto) {
    return new ResponseEntity<>(recurrentTransactionService.create(requestDto), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<RecurrentTransactionDto> update(@PathVariable Long id,
      @RequestBody @Valid RecurrentTransactionRequestDto requestDto) {
    return new ResponseEntity<>(recurrentTransactionService.update(id, requestDto), HttpStatus.OK);
  }

  @PatchMapping("/{id}/pause")
  public ResponseEntity<RecurrentTransactionDto> pause(@PathVariable Long id) {
    return new ResponseEntity<>(recurrentTransactionService.pause(id), HttpStatus.OK);
  }

  @PatchMapping("/{id}/resume")
  public ResponseEntity<RecurrentTransactionDto> resume(@PathVariable Long id) {
    return new ResponseEntity<>(recurrentTransactionService.resume(id), HttpStatus.OK);
  }

  @PatchMapping("/{id}/cancel")
  public ResponseEntity<RecurrentTransactionDto> cancel(@PathVariable Long id) {
    return new ResponseEntity<>(recurrentTransactionService.cancel(id), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    recurrentTransactionService.delete(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
