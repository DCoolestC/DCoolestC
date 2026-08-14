<?php
/**
 * Plugin Name: iSokoVibe Push Notifications
 * Description: Sends a Firebase Cloud Messaging push notification to the iSokoVibe Android app whenever a post is published.
 * Version: 1.0.0
 * Author: iSokoVibe
 * License: GPL-2.0-or-later
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit; // No direct access.
}

define( 'ISOKOVIBE_PUSH_OPTION', 'isokovibe_push_settings' );
// Must match the FCM topic the Android app subscribes to
// (PushNotificationManager.TOPIC in the app repo).
define( 'ISOKOVIBE_PUSH_TOPIC', 'isokovibe_new_music' );

// ---------------------------------------------------------------------
// Settings page: Settings > iSokoVibe Push
// ---------------------------------------------------------------------

add_action( 'admin_menu', function () {
	add_options_page(
		'iSokoVibe Push Notifications',
		'iSokoVibe Push',
		'manage_options',
		'isokovibe-push',
		'isokovibe_push_render_settings_page'
	);
} );

function isokovibe_push_get_settings() {
	return wp_parse_args(
		get_option( ISOKOVIBE_PUSH_OPTION, array() ),
		array(
			'service_account_json' => '',
			'post_types'           => array( 'post' ),
			'title_template'       => 'New on iSokoVibe',
			'body_template'        => '{title}',
			'enabled'              => false,
		)
	);
}

function isokovibe_push_render_settings_page() {
	if ( ! current_user_can( 'manage_options' ) ) {
		return;
	}

	$notice = '';

	// Save settings.
	if ( isset( $_POST['isokovibe_push_nonce'] )
		&& wp_verify_nonce( $_POST['isokovibe_push_nonce'], 'isokovibe_push_save' ) ) {
		$settings = array(
			'service_account_json' => wp_unslash( $_POST['service_account_json'] ?? '' ),
			'post_types'           => array_map( 'sanitize_text_field', wp_unslash( $_POST['post_types'] ?? array( 'post' ) ) ),
			'title_template'       => sanitize_text_field( wp_unslash( $_POST['title_template'] ?? 'New on iSokoVibe' ) ),
			'body_template'        => sanitize_text_field( wp_unslash( $_POST['body_template'] ?? '{title}' ) ),
			'enabled'              => isset( $_POST['enabled'] ),
		);
		update_option( ISOKOVIBE_PUSH_OPTION, $settings );
		$notice = '<div class="updated"><p>Settings saved.</p></div>';
	}

	// Send a test notification.
	if ( isset( $_POST['isokovibe_push_test'] )
		&& isset( $_POST['isokovibe_push_test_nonce'] )
		&& wp_verify_nonce( $_POST['isokovibe_push_test_nonce'], 'isokovibe_push_test' ) ) {
		$result = isokovibe_push_send( 'Test notification', 'This is a test from iSokoVibe.com.ng' );
		if ( is_wp_error( $result ) ) {
			$notice = '<div class="error"><p>' . esc_html( $result->get_error_message() ) . '</p></div>';
		} else {
			$notice = '<div class="updated"><p>Test notification sent.</p></div>';
		}
	}

	$settings = isokovibe_push_get_settings();
	?>
	<div class="wrap">
		<h1>iSokoVibe Push Notifications</h1>
		<p>Sends a push notification to the iSokoVibe Android app (via Firebase Cloud Messaging) whenever a post is published.</p>
		<?php echo $notice; // phpcs:ignore -- built from escaped fragments above. ?>

		<form method="post">
			<?php wp_nonce_field( 'isokovibe_push_save', 'isokovibe_push_nonce' ); ?>
			<table class="form-table">
				<tr>
					<th><label for="isokovibe-enabled">Enabled</label></th>
					<td><input type="checkbox" id="isokovibe-enabled" name="enabled" <?php checked( $settings['enabled'] ); ?> /></td>
				</tr>
				<tr>
					<th><label for="isokovibe-service-account">Firebase service account JSON</label></th>
					<td>
						<textarea id="isokovibe-service-account" name="service_account_json" rows="8" cols="60"
							placeholder='{"type": "service_account", ...}'><?php echo esc_textarea( $settings['service_account_json'] ); ?></textarea>
						<p class="description">
							Firebase console &rarr; Project settings &rarr; Service accounts &rarr; Generate new
							private key. Paste the full downloaded JSON file's contents here. (This is different
							from <code>google-services.json</code> — that one goes in the Android app, not here.)
						</p>
					</td>
				</tr>
				<tr>
					<th><label for="isokovibe-title">Notification title</label></th>
					<td><input type="text" id="isokovibe-title" name="title_template" value="<?php echo esc_attr( $settings['title_template'] ); ?>" class="regular-text" /></td>
				</tr>
				<tr>
					<th><label for="isokovibe-body">Notification body</label></th>
					<td>
						<input type="text" id="isokovibe-body" name="body_template" value="<?php echo esc_attr( $settings['body_template'] ); ?>" class="regular-text" />
						<p class="description">Use <code>{title}</code> to insert the post title.</p>
					</td>
				</tr>
				<tr>
					<th>Post types that trigger a notification</th>
					<td>
						<?php foreach ( get_post_types( array( 'public' => true ), 'objects' ) as $post_type ) : ?>
							<label style="display:block">
								<input type="checkbox" name="post_types[]" value="<?php echo esc_attr( $post_type->name ); ?>"
									<?php checked( in_array( $post_type->name, $settings['post_types'], true ) ); ?> />
								<?php echo esc_html( $post_type->labels->name ); ?>
							</label>
						<?php endforeach; ?>
					</td>
				</tr>
			</table>
			<?php submit_button( 'Save settings' ); ?>
		</form>

		<h2>Test</h2>
		<p>Sends one test push to every device currently subscribed (i.e. everyone with "Notify me about new music" turned on in the app).</p>
		<form method="post">
			<?php wp_nonce_field( 'isokovibe_push_test', 'isokovibe_push_test_nonce' ); ?>
			<input type="hidden" name="isokovibe_push_test" value="1" />
			<?php submit_button( 'Send a test notification', 'secondary' ); ?>
		</form>
	</div>
	<?php
}

// ---------------------------------------------------------------------
// Trigger: fires once when a post transitions into "publish" status.
// ---------------------------------------------------------------------

add_action( 'transition_post_status', function ( $new_status, $old_status, $post ) {
	if ( 'publish' !== $new_status || 'publish' === $old_status ) {
		return; // Only care about the moment something goes live.
	}

	$settings = isokovibe_push_get_settings();
	if ( ! $settings['enabled'] || ! in_array( $post->post_type, $settings['post_types'], true ) ) {
		return;
	}

	$title = $settings['title_template'];
	$body  = str_replace( '{title}', get_the_title( $post ), $settings['body_template'] );

	isokovibe_push_send( $title, $body, array( 'url' => get_permalink( $post ) ) );
}, 10, 3 );

// ---------------------------------------------------------------------
// FCM HTTP v1 sender — plain PHP + WordPress HTTP API, no Composer
// dependency, so it runs on ordinary shared/managed WordPress hosting.
// ---------------------------------------------------------------------

function isokovibe_push_send( $title, $body, $data = array() ) {
	$settings = isokovibe_push_get_settings();
	$json     = $settings['service_account_json'];
	if ( empty( $json ) ) {
		return new WP_Error( 'isokovibe_push', 'No Firebase service account configured yet.' );
	}

	$service_account = json_decode( $json, true );
	if ( ! $service_account
		|| empty( $service_account['project_id'] )
		|| empty( $service_account['private_key'] )
		|| empty( $service_account['client_email'] ) ) {
		return new WP_Error( 'isokovibe_push', 'That doesn\'t look like a valid Firebase service account JSON file.' );
	}

	$access_token = isokovibe_push_get_access_token( $service_account );
	if ( is_wp_error( $access_token ) ) {
		return $access_token;
	}

	$message = array(
		'message' => array(
			'topic'        => ISOKOVIBE_PUSH_TOPIC,
			'notification' => array(
				'title' => $title,
				'body'  => $body,
			),
			'data'         => array_map( 'strval', $data ),
		),
	);

	$response = wp_remote_post(
		"https://fcm.googleapis.com/v1/projects/{$service_account['project_id']}/messages:send",
		array(
			'headers' => array(
				'Authorization' => 'Bearer ' . $access_token,
				'Content-Type'  => 'application/json',
			),
			'body'    => wp_json_encode( $message ),
			'timeout' => 15,
		)
	);

	if ( is_wp_error( $response ) ) {
		return $response;
	}

	$code = wp_remote_retrieve_response_code( $response );
	if ( $code < 200 || $code >= 300 ) {
		return new WP_Error( 'isokovibe_push', 'FCM error ' . $code . ': ' . wp_remote_retrieve_body( $response ) );
	}

	return true;
}

/**
 * Exchanges the service account for a short-lived OAuth2 access token,
 * by hand-signing a JWT — this is what the Firebase Admin SDK does under
 * the hood, done here in plain PHP so no Composer packages are required.
 */
function isokovibe_push_get_access_token( $service_account ) {
	$now = time();

	$header = array(
		'alg' => 'RS256',
		'typ' => 'JWT',
	);
	$claims = array(
		'iss'   => $service_account['client_email'],
		'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
		'aud'   => 'https://oauth2.googleapis.com/token',
		'iat'   => $now,
		'exp'   => $now + 3600,
	);

	$signing_input = isokovibe_push_base64url( wp_json_encode( $header ) )
		. '.' . isokovibe_push_base64url( wp_json_encode( $claims ) );

	$private_key = openssl_pkey_get_private( $service_account['private_key'] );
	if ( ! $private_key ) {
		return new WP_Error( 'isokovibe_push', 'Could not read the service account\'s private key.' );
	}

	$signature = '';
	$signed    = openssl_sign( $signing_input, $signature, $private_key, OPENSSL_ALGO_SHA256 );
	if ( ! $signed ) {
		return new WP_Error( 'isokovibe_push', 'Failed to sign the auth request.' );
	}

	$jwt = $signing_input . '.' . isokovibe_push_base64url( $signature );

	$response = wp_remote_post(
		'https://oauth2.googleapis.com/token',
		array(
			'body'    => array(
				'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
				'assertion'  => $jwt,
			),
			'timeout' => 15,
		)
	);

	if ( is_wp_error( $response ) ) {
		return $response;
	}

	$body = json_decode( wp_remote_retrieve_body( $response ), true );
	if ( empty( $body['access_token'] ) ) {
		return new WP_Error( 'isokovibe_push', 'Could not get an access token: ' . wp_remote_retrieve_body( $response ) );
	}

	return $body['access_token'];
}

function isokovibe_push_base64url( $data ) {
	return rtrim( strtr( base64_encode( $data ), '+/', '-_' ), '=' );
}
