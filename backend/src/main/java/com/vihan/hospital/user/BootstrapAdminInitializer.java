package com.vihan.hospital.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.common.error.ApiException;
import com.vihan.hospital.config.AppProperties;
import com.vihan.hospital.security.Passwords;

/**
 * First start in any environment: if there is no active IT Admin, creates user {@code admin} with
 * the BOOTSTRAP_ADMIN_PASSWORD from .env (DESIGN.md §9.4). The password must be changed at first
 * login, and is never stored in SQL scripts.
 */
@Component
@Order(1)
public class BootstrapAdminInitializer implements ApplicationRunner {

	static final String USERNAME = "admin";
	private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuditService audit;
	private final TransactionTemplate tx;
	private final String password;

	public BootstrapAdminInitializer(AppUserRepository users, PasswordEncoder passwordEncoder, AuditService audit,
			TransactionTemplate tx, AppProperties props) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.audit = audit;
		this.tx = tx;
		this.password = props.auth().bootstrapAdminPassword();
	}

	@Override
	public void run(ApplicationArguments args) {
		tx.executeWithoutResult(status -> createIfNeeded());
	}

	private void createIfNeeded() {
		if (users.countByRoleAndActiveTrue(Role.IT_ADMIN) > 0) {
			return;
		}
		if (password == null || password.isBlank()) {
			log.warn("No active IT Admin account exists. Set BOOTSTRAP_ADMIN_PASSWORD in .env and restart to create '{}'.", USERNAME);
			return;
		}
		if (users.existsByUsername(USERNAME)) {
			log.warn("No active IT Admin, but user '{}' already exists (inactive?). Fix it in the database.", USERNAME);
			return;
		}
		try {
			Passwords.checkPolicy(password);
		}
		catch (ApiException e) {
			log.error("BOOTSTRAP_ADMIN_PASSWORD is too weak ({}). IT Admin not created.", e.getMessage());
			return;
		}
		AppUser admin = new AppUser(USERNAME, passwordEncoder.encode(password));
		admin.updateProfile("IT Admin", Role.IT_ADMIN, null, null, null, null, true);
		users.save(admin);
		audit.recordFor(null, AuditAction.USER_CREATED, "USER", admin.getId(), "bootstrap IT Admin");
		log.info("Created bootstrap IT Admin '{}'. The password must be changed at first login.", USERNAME);
	}

}
