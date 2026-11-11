package edu.gmu.cs321.events_resources.controller;

import edu.gmu.cs321.events_resources.common.PagedResponse;
import edu.gmu.cs321.events_resources.model.Opportunity;
import edu.gmu.cs321.events_resources.service.OpportunityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/opportunities")
public class OpportunityController {

	private final OpportunityService service;

	public OpportunityController(OpportunityService service) {
		this.service = service;
	}

	/**
	 * GET /opportunities?page=&pageSize=&q=&type=&includeClosed=
	 * Examples: /opportunities?type=Internship · /opportunities?q=capital%20one
	 */
	@GetMapping
	public PagedResponse<Opportunity> list(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int pageSize,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String type,
			@RequestParam(defaultValue = "false") boolean includeClosed) {
		return service.list(page, pageSize, q, type, includeClosed);
	}

	@GetMapping("/{id}")
	public Opportunity get(@PathVariable long id) {
		return service.get(id);
	}

	@PostMapping
	public ResponseEntity<Opportunity> create(@RequestBody Opportunity opportunity) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(opportunity));
	}

	@PutMapping("/{id}")
	public Opportunity update(@PathVariable long id, @RequestBody Opportunity opportunity) {
		return service.update(id, opportunity);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
